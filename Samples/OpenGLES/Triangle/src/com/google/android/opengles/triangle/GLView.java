/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.opengles.triangle;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.Writer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying an OpenGL animation.  This allows the animation to run in a
 * separate thread, without requiring that it be driven by the update mechanism
 * of the view hierarchy.
 *
 * The application-specific rendering code is delegated to a GLView.Renderer
 * instance.
 */
class GLView extends SurfaceView implements SurfaceHolder.Callback {

    SurfaceHolder mHolder;

    private GLThread mGLThread;
    private Renderer mRenderer;

    GLView(Context context) {
        super(context);
        init();
    }

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    public void setRenderer(Renderer renderer) {
        mRenderer = renderer;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, start our drawing thread.
        mGLThread = new GLThread(mRenderer);
        mGLThread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
        mGLThread.requestExitAndWait();
        mGLThread = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Surface size or format has changed. This should not happen in this
        // example.
        mGLThread.onWindowResize(w, h);
    }

    // ----------------------------------------------------------------------

    /**
     * A generic renderer interface.
     */
    interface Renderer {
        /**
         * @return the EGL configuration specification desired by the renderer.
         */
        int[] getConfigSpec();
        /**
         * Called whenever the OpenGL ES surface is initialized. Reinitialize
         * your OpenGL state here.
         * @param gl
         * @param width
         * @param height
         */
        void sizeChanged(GL10 gl, int width, int height);
        /**
         * Draw the current frame.
         * @param gl
         */
        void drawFrame(GL10 gl);
    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing.
     *
     */

    class GLThread extends Thread {
        GLThread(Renderer renderer) {
            super();
            mDone = false;
            mWidth = 0;
            mHeight = 0;
            mRenderer = renderer;
        }

        @Override
        public void run() {
            /*
             * Get an EGL instance
             */
            EGL10 egl = (EGL10) EGLContext.getEGL();

            /*
             * Get to the default display.
             */
            EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            /*
             * We can now initialize EGL for that display
             */
            int[] version = new int[2];
            egl.eglInitialize(dpy, version);

            /*
             * Specify a configuration for our opengl session
             * and grab the first configuration that matches is
             */
            int[] configSpec = mRenderer.getConfigSpec();

            EGLConfig[] configs = new EGLConfig[1];
            int[] num_config = new int[1];
            egl.eglChooseConfig(dpy, configSpec, configs, 1, num_config);
            EGLConfig config = configs[0];

            /*
            * Create an OpenGL ES context. This must be done only once, an
            * OpenGL context is a somewhat heavy object.
            */
            EGLContext context = egl.eglCreateContext(dpy, config,
                    EGL10.EGL_NO_CONTEXT, null);

            EGLSurface surface = null;
            GL10 gl = null;

            /*
             * This is our main activity thread's loop, we go until
             * asked to quit.
             */
            while (!mDone) {

                /*
                 *  Update the asynchronous state (window size, key events)
                 */
                int w, h;
                boolean changed;
                synchronized (this) {
                    changed = mSizeChanged;
                    w = mWidth;
                    h = mHeight;
                    mSizeChanged = false;
                }

                if (changed) {

                    /*
                     *  The window size has changed, so we need to create a new
                     *  surface.
                     */
                    if (surface != null) {

                        /*
                         * Unbind and destroy the old EGL surface, if
                         * there is one.
                         */
                        egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE,
                                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                        egl.eglDestroySurface(dpy, surface);
                    }

                    /*
                     * Create an EGL surface we can render into.
                     */
                    surface = egl.eglCreateWindowSurface(dpy, config, mHolder,
                            null);

                    /*
                     * Before we can issue GL commands, we need to make sure
                     * the context is current and bound to a surface.
                     */
                    egl.eglMakeCurrent(dpy, surface, surface, context);

                    /*
                     * Get to the appropriate GL interface.
                     * This is simply done by casting the GL context to either
                     * GL10 or GL11.
                     */
                    gl = (GL10) context.getGL();

                    if (mRenderer != null) {
                        mRenderer.sizeChanged(gl, w, h);
                    }
                }
                /* draw a frame here */
                if (mRenderer != null) {
                    mRenderer.drawFrame(gl);
                }

                /*
                 * Once we're done with GL, we need to call swapBuffers()
                 * to instruct the system to display the rendered frame
                 */
                egl.eglSwapBuffers(dpy, surface);

                /*
                 * Always check for EGL_CONTEXT_LOST, which means the context
                 * and all associated data were lost (For instance because
                 * the device went to sleep). We need to quit immediately.
                 */
                if (egl.eglGetError() == EGL11.EGL_CONTEXT_LOST) {
                    // we lost the gpu, quit immediately
                    Context c = getContext();
                    if (c instanceof Activity) {
                        ((Activity) c).finish();
                    }
                }
            }

            /*
             * clean-up everything...
             */
            egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
            egl.eglDestroySurface(dpy, surface);
            egl.eglDestroyContext(dpy, context);
            egl.eglTerminate(dpy);
        }

        public void onWindowResize(int w, int h) {
            synchronized (this) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
            }
        }

        public void requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            mDone = true;
            try {
                join();
            } catch (InterruptedException ex) {
            }
        }
    }

    private boolean mDone;
    private boolean mSizeChanged = true;
    private int mWidth;
    private int mHeight;
}


class LogWriter extends Writer {

    @Override
    public void close() {
        flushBuilder();
    }

    @Override
    public void flush() {
        flushBuilder();
    }

    @Override
    public void write(char[] buf, int offset, int count) {
        for (int i = 0; i < count; i++) {
            char c = buf[offset + i];
            if (c == '\n') {
                flushBuilder();
            } else {
                mBuilder.append(c);
            }
        }
    }

    private void flushBuilder() {
        if (mBuilder.length() > 0) {
            mBuilder.delete(0, mBuilder.length());
        }
    }

    private StringBuilder mBuilder = new StringBuilder();
}
