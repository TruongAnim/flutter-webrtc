package com.cloudwebrtc.webrtc.effect;

import android.util.Log;

import com.cloudwebrtc.webrtc.effect.filter.FrameImageFilter;
import com.cloudwebrtc.webrtc.effect.filter.GPUImageFilterWrapper;
import com.cloudwebrtc.webrtc.effect.filter.MediaEffectFilter;
import com.cloudwebrtc.webrtc.effect.format.YuvByteBufferDumper;
import com.cloudwebrtc.webrtc.effect.format.YuvByteBufferReader;

import org.webrtc.GlUtil;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFrame;

import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class RTCVideoEffector {

    public static final String TAG = RTCVideoEffector.class.getSimpleName();

    public RTCVideoEffector() {
    }

    private VideoEffectorContext context = new VideoEffectorContext();
    private List<FrameImageFilter> filters = new ArrayList<>();
    private boolean enabled = true;

    private YuvByteBufferReader yuvBytesReader;
    private YuvByteBufferDumper yuvBytesDumper;

    private SurfaceTextureHelper helper;

    void init(SurfaceTextureHelper helper) {

        VideoEffectorLogger.d(TAG, "init");

        this.helper = helper;

        yuvBytesReader = new YuvByteBufferReader();
        yuvBytesReader.init();

        yuvBytesDumper = new YuvByteBufferDumper();
        yuvBytesDumper.init();


        for (FrameImageFilter filter : filters) {
            filter.init();
        }

        GlUtil.checkNoGLES2Error("RTCVideoEffector.init");
    }

    public void addFilter(FrameImageFilter filter) {
        this.filters.add(filter);
    }

    // @link EffectFactory
    public void addMediaEffectFilter(String name) {
        addMediaEffectFilter(name, null);
    }

    public void addMediaEffectFilter(String name,
                                     MediaEffectFilter.Listener listener) {
        VideoEffectorLogger.d(TAG, "addMediaEffectFilter: " + name +
                ", listener: " + listener);
        this.filters.add(new MediaEffectFilter(name, listener));
    }

    public void addGPUImageFilter(GPUImageFilter filter) {
        VideoEffectorLogger.d(TAG, "addGPUImageFilter: " + filter.toString());
        this.filters.add(new GPUImageFilterWrapper(filter));
    }

    public void addGPUImageFilter(GPUImageFilter filter,
                                  GPUImageFilterWrapper.Listener listener) {
        VideoEffectorLogger.d(TAG, "addGPUImageFilter: " + filter.toString() +
                ", listener: " + listener);
        this.filters.add(new GPUImageFilterWrapper(filter, listener));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    VideoFrame.I420Buffer processByteBufferFrame(VideoFrame.I420Buffer i420Buffer, int rotation, long timestamp) {

        if (!needToProcessFrame()) {
            return i420Buffer;
        }

        // TODO: 还有一种方法可以转换为直接
        if (!i420Buffer.getDataY().isDirect() || !i420Buffer.getDataU().isDirect() || !i420Buffer.getDataV().isDirect()) {
            return i420Buffer;
        }
        int width = i420Buffer.getWidth();
        int height = i420Buffer.getHeight();
        int strideY = i420Buffer.getStrideY();
        int strideU = i420Buffer.getStrideU();
        int strideV = i420Buffer.getStrideV();

        context.updateFrameInfo(width, height, rotation, timestamp);

        int stepTextureId = yuvBytesReader.read(i420Buffer);

        // Video frames may rotate.
        // For effects applied uniformly across the entire image, such as grayscale and sepia filters, this is not an issue.
        // It is difficult to use effects that require specifying coordinates.

        // Therefore, in some cases, some rotation correction may be needed before and after filtering.
        // However, texture copying will occur twice.
        // I hope to be able to enable/disable this feature so that it is not used when not needed.

        if (context.getFrameInfo().isRotated()) {
            // TODO
        }

        for (FrameImageFilter filter : filters) {
            if (filter.isEnabled()) {
                stepTextureId = filter.filter(context, stepTextureId);
            }
        }

        if (context.getFrameInfo().isRotated()) {
            // TODO
        }

        return yuvBytesDumper.dump(stepTextureId, width, height, strideY, strideU, strideV);
    }

    boolean needToProcessFrame() {
        if (!enabled) {
            return false;
        }
        if (filters.size() > 0) {
            for (FrameImageFilter filter : this.filters) {
                if (filter.isEnabled()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dispose() {
        if (this.helper != null) {
            return;
        }
        ThreadUtils.invokeAtFrontUninterruptibly(this.helper.getHandler(), () ->
                disposeInternal()
        );
    }

    private void disposeInternal() {
        for (FrameImageFilter filter : filters) {
            filter.dispose();
        }
        yuvBytesReader.dispose();
        yuvBytesDumper.dispose();
    }
}
