import static org.lwjgl.opengl.EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glFramebufferTexture2DEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenFramebuffersEXT;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glShadeModel;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glValidateProgram;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

import javax.swing.JFileChooser;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public class Engine {
    private long lastFrame;

    private int fps;
    private long lastFPS;

    private boolean vsyncEnabled = true;

    private Dimension screenSize;

    private int advanceShaderProgram;
    private int drawShaderProgram;

    private int[] framebuffer = new int[2], colorbuffer = new int[2];

    private int width, height;
    private int fieldWidth = 5000, fieldHeight = 5000;

    private int targetTextureID = 0;
    private float scale = 1;
    private float xOffset = 0, yOffset = 0;

    private int fill = 1;
    private boolean alive = true;

    private JFileChooser fc;
    private String script;

    Engine() {
        fc = new JFileChooser(Paths.get("").toAbsolutePath().toString() + "/scripts");
        script = "int step() { return 0; } vec4 color(float cell) { return vec4(0.0, 0.0, 0.0, 0.0); } int fill() { return 0; }";
    }

    public void start() {
        try {
            screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            width = screenSize.width / 2;
            height = screenSize.height / 2;

            fieldWidth = screenSize.width;
            fieldHeight = screenSize.height;

            Display.setTitle("Project Cell");
            Display.setDisplayMode(new DisplayMode(width, height));
            Display.setResizable(true);
            Display.setVSyncEnabled(vsyncEnabled);
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        init();
        getDelta(); // call once before loop to initialize lastFrame
        lastFPS = getTime(); // call before loop to initialize FPS timer

        while (!Display.isCloseRequested()) {
            update();
            advance();
            render();

            Display.update();

            if (vsyncEnabled)
                Display.sync(60);
        }

        Display.destroy();
    }

    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("FPS: " + fps);
            fps = 0;
            lastFPS += 1000;
        }

        fps++;
    }

    public void update() {
        while (Mouse.next()) {
            if (Mouse.isButtonDown(0)) {
                xOffset += Mouse.getDX();
                yOffset += Mouse.getDY();
            }

            if (Mouse.getDWheel() != 0)
                if (Mouse.getEventDWheel() > 0)
                    scale *= 1.1;
                else
                    scale /= 1.1;
        }

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState())
                switch (Keyboard.getEventKey()) {
                    case Keyboard.KEY_F11:
                        setDisplayMode(Display.isFullscreen() ? width : screenSize.width, Display.isFullscreen() ? height : screenSize.height, !Display.isFullscreen());
                        break;

                    case Keyboard.KEY_V:
                        vsyncEnabled ^= true;
                        Display.setVSyncEnabled(vsyncEnabled);
                        break;

                    case Keyboard.KEY_ESCAPE:
                        if (Display.isFullscreen())
                            setDisplayMode(width, height, false);
                        else {
                            Display.destroy();
                            System.exit(0);
                        }
                        break;

                    case Keyboard.KEY_R:
                        fill = 1;
                        break;

                    case Keyboard.KEY_SPACE:
                        alive ^= true;
                        break;

                    case Keyboard.KEY_BACK:
                        scale = 1;
                        xOffset = yOffset = 0;
                        break;

                    case Keyboard.KEY_LEFT:
                        xOffset -= 10;
                        break;

                    case Keyboard.KEY_RIGHT:
                        xOffset += 10;
                        break;

                    case Keyboard.KEY_DOWN:
                        yOffset -= 10;
                        break;

                    case Keyboard.KEY_UP:
                        yOffset += 10;
                        break;

                    case Keyboard.KEY_MINUS:
                        scale /= 1.1;
                        break;

                    case Keyboard.KEY_EQUALS:
                        scale *= 1.1;
                        break;

                    case Keyboard.KEY_O:
                        if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                            break;
                        loadScript(fc.getSelectedFile());
                        break;
                }
        }

        if (alive)
            targetTextureID ^= 1;

        updateFPS();
    }

    public void resize(int w, int h) {
        fieldWidth = w;
        fieldHeight = h;
        initField();
    }

    private void loadScript(File file) {
        BufferedReader reader = null;

        StringBuilder scriptBuilder = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null)
                scriptBuilder.append(line).append('\n');
        } catch (IOException e) {
            System.err.println("Script wasn't loaded properly.");
            e.printStackTrace();
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        script = scriptBuilder.toString();

        loadAdvanceShader();
        loadDrawShader();

        fill = 1;
    }

    private void loadAdvanceShader() {
        advanceShaderProgram = glCreateProgram();

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

        StringBuilder vertexShaderSource = new StringBuilder();
        StringBuilder fragmentShaderSource = new StringBuilder();

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader("src/vertex.glsl"));
            String line;
            while ((line = reader.readLine()) != null)
                vertexShaderSource.append(line).append('\n');
        } catch (IOException e) {
            System.err.println("Advance vertex shader wasn't loaded properly.");
            e.printStackTrace();
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            reader = new BufferedReader(new FileReader("src/advance.glsl"));
            String line;
            while ((line = reader.readLine()) != null) {
                fragmentShaderSource.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Advance fragment shader wasn't loaded properly.");
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        fragmentShaderSource.append(script);
        fragmentShaderSource.append("void main() { if(init == 1) gl_FragColor = vec4(float(fill()) / 255.0, 0.0, 0.0, 0.0); else gl_FragColor = vec4(float(step()) / 255.0, 0.0, 0.0, 0.0); }");

        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
            System.err.println("Error compiling advance vertex shader.");

        String info = glGetShaderInfoLog(vertexShader, 1000);

        if (!info.isEmpty())
            System.err.println(info);

        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
            System.err.println("Error compiling advance fragment shader.");

        info = glGetShaderInfoLog(fragmentShader, 1000);

        if (!info.isEmpty())
            System.err.println(info);

        glAttachShader(advanceShaderProgram, vertexShader);
        glAttachShader(advanceShaderProgram, fragmentShader);

        glLinkProgram(advanceShaderProgram);
        glValidateProgram(advanceShaderProgram);
    }

    private void loadDrawShader() {
        drawShaderProgram = glCreateProgram();

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

        StringBuilder vertexShaderSource = new StringBuilder();
        StringBuilder fragmentShaderSource = new StringBuilder();

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader("src/vertex.glsl"));
            String line;
            while ((line = reader.readLine()) != null)
                vertexShaderSource.append(line).append('\n');
        } catch (IOException e) {
            System.err.println("Draw vertex shader wasn't loaded properly.");
            e.printStackTrace();
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            reader = new BufferedReader(new FileReader("src/draw.glsl"));
            String line;
            while ((line = reader.readLine()) != null) {
                fragmentShaderSource.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Draw fragment shader wasn't loaded properly.");
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        fragmentShaderSource.append(script);
        fragmentShaderSource.append("void main() { gl_FragColor = color(texture2D(texture, gl_TexCoord[0].st).r * 255.0); }");

        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
            System.err.println("Error compiling draw vertex shader.");

        String info = glGetShaderInfoLog(vertexShader, 1000);

        if (!info.isEmpty())
            System.err.println(info);

        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
            System.err.println("Error compiling draw fragment shader.");

        info = glGetShaderInfoLog(fragmentShader, 1000);

        if (!info.isEmpty())
            System.err.println(info);

        glAttachShader(drawShaderProgram, vertexShader);
        glAttachShader(drawShaderProgram, fragmentShader);

        glLinkProgram(drawShaderProgram);
        glValidateProgram(drawShaderProgram);
    }

    private void initField() {
        if (!GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            System.out.println("FBO not supported.");
            System.exit(0);
        } else {
            for (int i = 0; i < 2; ++i) {
                framebuffer[i] = glGenFramebuffersEXT();
                colorbuffer[i] = glGenTextures();

                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebuffer[i]);

                glBindTexture(GL_TEXTURE_2D, colorbuffer[i]);

                glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, fieldWidth, fieldHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, colorbuffer[i], 0);
            }

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        }
    }

    private void init() {
        loadAdvanceShader();
        loadDrawShader();
        initField();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL11.GL_LIGHTING);
        glShadeModel(GL_SMOOTH);
    }

    private void advance() {
        if (!alive)
            return;

        glViewport(0, 0, fieldWidth, fieldHeight);

        glBindTexture(GL_TEXTURE_2D, 0);

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebuffer[targetTextureID]);

        glUseProgram(advanceShaderProgram);

        glBindTexture(GL_TEXTURE_2D, colorbuffer[targetTextureID ^ 1]);
        glUniform1i(glGetUniformLocation(advanceShaderProgram, "texture"), 0);

        glUniform1i(glGetUniformLocation(advanceShaderProgram, "init"), fill);

        glUniform1f(glGetUniformLocation(advanceShaderProgram, "width"), fieldWidth);
        glUniform1f(glGetUniformLocation(advanceShaderProgram, "height"), fieldHeight);

        glUniform1f(glGetUniformLocation(advanceShaderProgram, "time"), System.nanoTime());

        if (fill == 1)
            fill = 0;

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0, fieldWidth, 0.0, fieldHeight, 1.0, -1.0);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glTranslatef(fieldWidth / 2.0f, fieldHeight / 2.0f, 0.0f);
        // glScalef(1.0f/* fieldWidth / 2.0f */, 1.0f/* fieldHeight / 2.0f */, 1.0f);

        drawQuad();

        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }

    private void render() {
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glViewport(0, 0, Display.getWidth(), Display.getHeight());

        glUseProgram(drawShaderProgram);

        glBindTexture(GL_TEXTURE_2D, colorbuffer[targetTextureID]);
        glUniform1i(glGetUniformLocation(drawShaderProgram, "texture"), 0);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), 1.0, -1.0);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glTranslatef(xOffset + Display.getWidth() / 2.0f, yOffset + Display.getHeight() / 2.0f, 0.f);
        glScalef(scale, scale, 1.0f);

        drawQuad();

        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);

        glFlush();
    }

    private void drawQuad() {
        glBegin(GL_QUADS);

        glTexCoord2f(0, 0);
        glVertex2f((float) -fieldWidth / 2, (float) -fieldHeight / 2);

        glTexCoord2f(1, 0);
        glVertex2f((float) fieldWidth / 2, (float) -fieldHeight / 2);

        glTexCoord2f(1, 1);
        glVertex2f((float) fieldWidth / 2, (float) fieldHeight / 2);

        glTexCoord2f(0, 1);
        glVertex2f((float) -fieldWidth / 2, (float) fieldHeight / 2);

        glEnd();
    }

    /**
     * Set the display mode to be used
     *
     * @param width      The width of the display required
     * @param height     The height of the display required
     * @param fullscreen True if we want fullscreen mode
     */
    private void setDisplayMode(int width, int height, boolean fullscreen) {

        // return if requested DisplayMode is already set
        if (Display.getDisplayMode().getWidth() == width && Display.getDisplayMode().getHeight() == height && Display.isFullscreen() == fullscreen) {
            return;
        }

        try {
            DisplayMode targetDisplayMode = null;

            if (fullscreen) {
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;

                for (int i = 0; i < modes.length; i++) {
                    DisplayMode current = modes[i];

                    if (current.getWidth() == width && current.getHeight() == height) {
                        if (targetDisplayMode == null || current.getFrequency() >= freq) {
                            if (targetDisplayMode == null || current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel()) {
                                targetDisplayMode = current;
                                freq = targetDisplayMode.getFrequency();
                            }
                        }

                        // if we've found a match for bpp and frequence against the
                        // original display mode then it's probably best to go for this one
                        // since it's most likely compatible with the monitor
                        if (current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel() && current.getFrequency() == Display.getDesktopDisplayMode().getFrequency()) {
                            targetDisplayMode = current;
                            break;
                        }
                    }
                }
            } else {
                targetDisplayMode = new DisplayMode(width, height);
            }

            if (targetDisplayMode == null) {
                System.err.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
                return;
            }

            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);

        } catch (LWJGLException e) {
            System.err.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
        }
    }

    /**
     * Calculate how many milliseconds have passed since last frame.
     *
     * @return milliseconds passed since last frame
     */
    private int getDelta() {
        long time = getTime();
        int delta = (int) (time - lastFrame);
        lastFrame = time;
        return delta;
    }

    /**
     * Get the accurate system time
     *
     * @return The system time in milliseconds
     */
    private long getTime() {
        return Sys.getTime() * 1000 / Sys.getTimerResolution();
    }
}
