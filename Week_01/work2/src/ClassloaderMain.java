import java.io.*;
import java.lang.reflect.Method;

public class ClassloaderMain {
    public static void main(String[] args) throws Exception {
        Class clazz = new customClassloader().findClass("Hello");
        Object obj = clazz.newInstance();
        Method method = clazz.getMethod("hello");
        method.invoke(obj);
    }
}

class customClassloader extends ClassLoader {

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] xlassByte = this.getByteClassByPath("Hello.xlass");
        byte[] classByte = this.reSetByteClass(xlassByte);
        Class clazz = defineClass(name, classByte, 0, classByte.length);
        return clazz;
    }

    private byte[] getByteClassByPath(String name) {
        InputStream in = ClassloaderMain.class.getClassLoader().getResourceAsStream(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int size;
            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
            }
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private byte[] reSetByteClass(byte[] xlassBytes) {
        byte[] resultByte = new byte[xlassBytes.length];
        for (int i = 0; i < xlassBytes.length; i++) {
            int xlassInt = 255 - (int) xlassBytes[i];
            resultByte[i] = (byte) xlassInt;
        }
        return resultByte;
    }
}