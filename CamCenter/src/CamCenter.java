/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CamCenter extends JPanel implements Runnable {

    private boolean useMJPGStream = true;
    private boolean motion = true;
    private int intentos =0;
    public String jpgURL = "http://81.92.254.3/axis-cgi/mjpg/video.cgi";
    public String mjpgURL = "http://87.101.127.24/axis-cgi/mjpg/video.cgi";
    private boolean terminado =false;
    DataInputStream dis;
    Font f = new Font("Courier", Font.PLAIN,  10);
    JPanel panel = new JPanel();
    JLabel picLabel = new JLabel("No hay conexion");

    private String Nombre = "Camara";
    private BufferedImage image = null;
    public boolean connected = false;
    HttpURLConnection huc = null;
    Component parent;
    JFrame frame = new JFrame("Display image");

    /** Creates a new instance of AxisCamera */
    public CamCenter(String b,String URL,JFrame fr) {
 
        mjpgURL = URL;
        frame = fr;
        Nombre = b;
        panel.add(picLabel);
        frame.add(panel);
        
    }

        public void Mostrar(JFrame frame, BufferedImage NombreArchivo) throws
    Exception {
            Graphics g=NombreArchivo.getGraphics();
            g.setFont(f);
            g.drawString(Nombre, 5, 10);

            picLabel.setText("");
            picLabel.setIcon(new ImageIcon(NombreArchivo));
            picLabel.invalidate();
            picLabel.validate();

       if (!frame.isShowing()){

       System.out.println("cerrado");
       terminado = true;
       frame.dispose();
       }
    }

    public void connect() {
        try {
            URL u = new URL(useMJPGStream ? mjpgURL : jpgURL);
            huc = (HttpURLConnection) u.openConnection();
            System.out.println(huc.getContentType());
            if(null==huc.getContentType() && intentos <3){
                intentos++;
                System.out.println("No Hay conexion con "+mjpgURL+" volviendo a intentar, intento "+ intentos);
                Thread.sleep(1000);
                connect();
            }else{

            InputStream is = huc.getInputStream();
            connected = true;
            BufferedInputStream bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);
            readStream();
            }
            disconnect();
        } catch (IOException e) { //incase no connection exists wait and try again, instead of printing the error
            try {
                huc.disconnect();
                Thread.sleep(60);
            } catch (InterruptedException ie) {
                huc.disconnect();
                if(intentos<3){
                connect();
                }
            }
            if(intentos<3){
            connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connected) {
                dis.close();
                connected = false;
                System.out.println("Cerrando conexion con" + mjpgURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paint(Graphics g) { //used to set the image on the panel
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }

    public void readStream() { //the basic method to continuously read the stream
        try {
            if (useMJPGStream) {
                while (!terminado) {
                    readMJPGStream();
                    int type = image.getType() == 0? BufferedImage.TYPE_INT_ARGB : image.getType();
                    Mostrar(frame,resizeImage(image,type));
                }
            } else {
                while (!terminado) {
                    connect();
                    readJPG();
                    parent.repaint();
                    disconnect();

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readMJPGStream() { //preprocess the mjpg stream to remove the mjpg encapsulation
       if(motion){
        readLine(4, dis); //discard the first 3 lines
        readJPG();
        readLine(1, dis); //discard the last two lines
        }else{
        readLine(3, dis); //discard the first 3 lines
        readJPG();
        readLine(2, dis); //discard the last two lines


        }
    }

    public void readJPG() { //read the embedded jpeg image
        try {
            JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(dis);
            image = decoder.decodeAsBufferedImage();
            //ImageIO.write((RenderedImage) image, "jpg", new File(String.format("imagen%d",d))); // sirve para crear el archivo
            //System.out.println("se guarda la foto");
            
            
        } catch (Exception e) {
                        System.out.println("Formato Invalido de MJPE");
            if(motion==false){
                motion=true;
                System.out.println("cambiando de modo a Formato Motion");
            }else{
                System.out.println("cambiando de modo a Formato no Motion");
                motion= false;
            }

            disconnect();
            connect();
            readStream();
        }
    }

    public void readLine(int n, DataInputStream dis) { //used to strip out the header lines
        for (int i = 0; i < n; i++) {
            readLine(dis);
        }
    }

    public void readLine(DataInputStream dis) {
        try {
            boolean end = false;
            String lineEnd = "\n"; //asegura el final de linea
            byte[] lineEndBytes = lineEnd.getBytes();
            byte[] byteBuf = new byte[lineEndBytes.length];

            while (!end) {
                dis.read(byteBuf, 0, lineEndBytes.length);
                String t = new String(byteBuf);
                //System.out.print(t); //imprime lo q se elimina
                if (t.equals(lineEnd)) {
                    end = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

        private BufferedImage resizeImage(BufferedImage originalImage, int type){
	BufferedImage resizedImage = new BufferedImage(320, 240, type);
	Graphics g = resizedImage.getGraphics();
	g.drawImage(originalImage, 0, 0, 320,240, null);
	g.dispose();

	return resizedImage;
    }

    public void run() {
        connect();
        disconnect();

    }

    public static JFrame Init(int cantidad){
       JFrame frame = new JFrame("Display image");
      

       switch(cantidad){
           case 1:
            frame.getContentPane().setLayout(new GridLayout(0,1));
           case 2:
            frame.getContentPane().setLayout(new GridLayout(0,2));
            frame.setSize(330*cantidad,280);
            break;
           case 3:
            frame.setSize(330*cantidad,280);
            frame.getContentPane().setLayout(new GridLayout(0,3));
            break;
           case 4:
            frame.getContentPane().setLayout(new GridLayout(2,3));
            frame.setSize(330*2,560);
            break;
            case 6:

           case 5:
               frame.setSize(330*3,560);
             frame.getContentPane().setLayout(new GridLayout(2,3));
             break;

           case 7:
            
           case 8:

           case 9:
               frame.getContentPane().setLayout(new GridLayout(3,3));
             frame.setSize(330*3,280*3);
             break;
        }
       frame.setVisible(true);
      
       
        return frame;
    }

 public static HashMap lector(String path){
            HashMap Camaras = new HashMap();
        try {

            int pos =0;
            File file = new File(path);
            BufferedReader reader = null;
            String text = null;
            reader = new BufferedReader(new FileReader(file));
            while ((text = reader.readLine()) != null) {
                String[] words = text.split(" ");
                int i =0;
                while (i < words.length){
                    camara Cam = new camara(words[i++],words[i++]);
                    Camaras.put(pos++,Cam);
                }
            }
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        }
        return Camaras;
    }

    public static void main(String[] args) {

       
       
       HashMap cams = CamCenter.lector(args[0]);
       JFrame frame= Init(cams.size());
       Iterator k =cams.values().iterator();
       while(k.hasNext()){
          camara c = (camara) k.next();
          CamCenter axPanel = new CamCenter(c.Nombre,c.URL,frame);
          new Thread(axPanel).start();
       }



    }
}
