import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Q2 extends Application {
    private static Scene scene, sceneW, sceneR, sceneG, sceneB, sceneD;
    private static WritableImage wImage, compressedImage;
    private static List<Short> dcR, dcG, dcB, rlcR, rlcG, rlcB;
    private static int[][] q;
    private static double[][] T;
    private static int rlcMaxLength = 9, w, h;

    @Override
    public void start(Stage stage) throws FileNotFoundException {
        stage.setTitle("PNG");

        Label label1 = new Label("Which PNG file do you want to read?");
        Button button1 = new Button("autumn.png");
        button1.setOnAction(e -> {
            readPNG("autumn.png");
            showSceneW(stage);
        });
        Button button2 = new Button("balloons.png");
        button2.setOnAction(e -> {
            readPNG("balloons.png");
            showSceneW(stage);
        });
        Button button3 = new Button("board.png");
        button3.setOnAction(e -> {
            readPNG("board.png");
            showSceneW(stage);
        });
        VBox layout1 = new VBox(20);
        layout1.getChildren().addAll(label1, button1, button2, button3);
        scene = new Scene(layout1, 300, 250);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String args[]) {
        launch(args);
    }

    public static void readPNG(String fileName) {
        try {
            //Creating an image
            Image image = new Image(new FileInputStream(fileName));
            int width = (int)image.getWidth();
            int height = (int)image.getHeight();
            width = width / 8 * 8;
            height = height / 8 * 8;
            //Creating a writable image
            wImage = new WritableImage(width, height);
            compressedImage = new WritableImage(width, height);
            //Reading color from the loaded image
            PixelReader pixelReader = image.getPixelReader();

            //getting the pixel writer
            PixelWriter writer = wImage.getPixelWriter();
            PixelWriter compressedWriter = compressedImage.getPixelWriter();
            int[][] rarray = new int[height][width];
            int[][] garray = new int[height][width];
            int[][] barray = new int[height][width];
            int[][] rCompressedarray = new int[height][width];
            int[][] gCompressedarray = new int[height][width];
            int[][] bCompressedarray = new int[height][width];
            //Reading the color of the image
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    //Retrieving the color of the pixel of the loaded image
                    Color color = pixelReader.getColor(x, y);
                    rarray[y][x] = (int)(255*color.getRed());
                    garray[y][x] = (int)(255*color.getGreen());
                    barray[y][x] = (int)(255*color.getBlue());
                    //Setting the color to the writable image
                    writer.setColor(x, y, color);
                }
            }

            T = new double[8][8];
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    double a = Math.sqrt(1.0/8);
                    if (i > 0)
                        a = Math.sqrt(2.0/8);
                    T[i][j] = a * Math.cos((2*j+1)*i*Math.PI/16);
                }
            }
            q = new int[8][8];
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int a = Math.max(i, j);
                    if (a == 0)
                        q[i][j] = 1;
                    else
                        q[i][j] = (int)Math.round(Math.pow(2, a-1));
                }
            }
            dcR = new ArrayList<>();
            dcG = new ArrayList<>();
            dcB = new ArrayList<>();
            rlcR = new ArrayList<>();
            rlcG = new ArrayList<>();
            rlcB = new ArrayList<>();

            for (int i = 0; i < height/8; i++) {
                for (int j = 0; j < width/8; j++) {
                    encode(extract(rarray, i, j), dcR, rlcR);
                    encode(extract(garray, i, j), dcG, rlcG);
                    encode(extract(barray, i, j), dcB, rlcB);
                }
            }

            short max = Short.MIN_VALUE, min = Short.MAX_VALUE;
            for (short i : dcR) {
                if (i > max)
                    max = i;
                if (i < min)
                    min = i;
                //System.out.print(i + " ");
            }
            System.out.println("\nmax: " + max + " min: " + min);
            max = Short.MIN_VALUE; min = Short.MAX_VALUE;
            for (short i : rlcR) {
                if (i > max)
                    max = i;
                if (i < min)
                    min = i;
                //System.out.print(i + " ");
            }
            System.out.println("\nmax: " + max + " min: " + min);

            save(width, height, dcR, dcG, dcB, rlcR, rlcG, rlcB);
            // clear all the data
            dcR.clear(); dcG.clear(); dcB.clear();
            rlcR.clear(); rlcG.clear(); rlcB.clear();

            // read the data from file
            load(dcR, dcG, dcB, rlcR, rlcG, rlcB);
            decodeMatrix2(height, width, rCompressedarray, rlcR, dcR);
            decodeMatrix2(height, width, gCompressedarray, rlcG, dcG);
            decodeMatrix2(height, width, bCompressedarray, rlcB, dcB);

            for (int i = 0; i < 40; i++) {
                for (int j = 0; j < 40; j++) {
                    //System.out.print(rarray[i][j]-rCompressedarray[i][j] + " ");
                }
                //System.out.println();
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color color = new Color(rCompressedarray[y][x]/255.0, gCompressedarray[y][x]/255.0, bCompressedarray[y][x]/255.0, 1.0);
                    //Setting the color to the writable image
                    compressedWriter.setColor(x, y, color);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showSceneW(Stage stage) {
        //Setting the view for the writable image
        ImageView imageView = new ImageView(wImage);
        ImageView compressedImageView = new ImageView(compressedImage);
        HBox layout1 = new HBox(20);
        layout1.getChildren().addAll(imageView, compressedImageView);
        sceneW = new Scene(layout1, wImage.getWidth()+compressedImage.getWidth(), wImage.getHeight());
        //Setting title to the Stage
        stage.setTitle("Original image and compressed image ");
        //Adding scene to the stage
        stage.setScene(sceneW);
        //Displaying the contents of the stage
        stage.show();
    }

    public static void encode(int[][] X, List<Short> dc, List<Short> rlc) {
        double[][] F = TXT(T, X);
        //print2DArray(F);

        short[][] Fhat = quantizationEncode(F, q);
        //print2DArray(Fhat);

        short[] zigzag = zigzagEncode(Fhat);
        dc.add(zigzag[0]);
        //print1DArray(zigzag);

        rlcEncode2(zigzag, rlc);
    }

    private static double[][] TXT(double[][] T, int[][] X) {
        double[][] result = new double[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                double r = 0;
                for (int ii = 0; ii < 8; ii++) {
                    for (int jj = 0; jj < 8; jj++) {
                        r = r + X[ii][jj] * T[i][ii] * T[j][jj];
                    }
                }
                result[i][j] = r;
            }
        }
        return result;
    }

    private static short[][] quantizationEncode(double[][] F, int[][] q) {
        short[][] Fhat = new short[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Fhat[i][j] = (short) Math.round(F[i][j] / q[i][j]);
            }
        }
        return Fhat;
    }

    private static short[] zigzagEncode(short[][] X) {
        short[] result = new short[64];
        int index = 0;
        for (int k = 0; k <= 14; k++) {
            if (k <= 7) {
                if (k % 2 == 0) {
                    for (int i = k, j = 0; i >= 0; i--,j++,index++) {
                        result[index] = X[i][j];
                    }
                } else {
                    for (int i = 0, j = k; i <= k; i++,j--,index++) {
                        result[index] = X[i][j];
                    }
                }
            } else {
                if (k % 2 == 0) {
                    for (int i = 7, j = k - i; j <= 7; i--,j++,index++) {
                        result[index] = X[i][j];
                    }
                } else {
                    for (int j = 7, i = k - j; i <= 7; i++,j--,index++) {
                        result[index] = X[i][j];
                    }
                }
            }
        }
        return result;
    }

    /*private static void rlcEncode(short[] array, List<Short> rlc) {
        short count = 0;
        for (int i = 1; i < 64; i++) {
            if (array[i] == 0) {
                count++;
            } else {
                rlc.add(count);
                rlc.add(array[i]);
                count = 0;
            }
        }
        rlc.add((short) 0);rlc.add((short) 0);
    }*/

    private static void rlcEncode2(short[] array, List<Short> rlc) {
        for (int i = 1; i < rlcMaxLength+1; i++) {
            rlc.add(array[i]);
        }
    }

    private static void print2DArray(double[][] array) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(array[i][j] + " ");
            }
            System.out.println();
        }
    }

    private static void print2DArray(int[][] array) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(array[i][j] + " ");
            }
            System.out.println();
        }
    }

    private static void print1DArray(int[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println();
    }

    /*private static void decodeMatrix(int height, int width, int[][] rCompressedarray, List<Short> rlcList, List<Short> dcList) {
        for (int i = 0; i < height/8; i++) {
            for (int j = 0; j < width/8; j++) {
                List<Integer> rlc = new ArrayList<>();
                while (!rlcList.isEmpty()) {
                    int first = rlcList.get(0);
                    int second = rlcList.get(1);
                    rlcList.remove(0);
                    rlcList.remove(0);
                    rlc.add(first);
                    rlc.add(second);
                    if (first == 0 && second == 0) {
                        break;
                    }
                }
                int[] rlcArray = new int[rlc.size()];
                for (int k = 0; k < rlcArray.length; k++) {
                    rlcArray[k] = rlc.get(k);
                }
                int[][] XDecoded = decode(dcList.get(0), rlcArray);
                dcList.remove(0);
                put(rCompressedarray, i, j, XDecoded);
            }
        }
    }*/

    private static void decodeMatrix2(int height, int width, int[][] rCompressedarray, List<Short> rlcList, List<Short> dcList) {
        for (int i = 0; i < height/8; i++) {
            for (int j = 0; j < width/8; j++) {
                int[] rlcArray = new int[rlcMaxLength];
                for (int k = 0; k < rlcMaxLength; k++) {
                    rlcArray[k] = rlcList.get(0);
                    rlcList.remove(0);
                }
                int[][] XDecoded = decode(dcList.get(0), rlcArray);
                dcList.remove(0);
                put(rCompressedarray, i, j, XDecoded);
            }
        }
    }

    private static int[][] decode(int dc, int[] rlc) {
        int[] zigzag = rlcDecode2(dc, rlc);
        //print1DArray(zigzag);

        int[][] Fhat = zigzagDecode(zigzag);
        //print2DArray(Fhat);

        int[][] FDecoded = quantizationDecode(Fhat, q);
        //print2DArray(FDecoded);

        int[][] X = TYT(T, FDecoded);
        //print2DArray(X);

        return X;
    }

    /*private static int[] rlcDecode(int dc, int[] rlc) {
        int[] zigzag = new int[64];
        zigzag[0] = dc;
        for (int i = 1, j = 0; i < 64; i++, j = j+2) {
            if (rlc[j+1] == 0) {
                while (i < 64) {
                    zigzag[i] = 0;
                    i++;
                }
                break;
            } else {
                for (int k = 0; k < rlc[j]; k++) {
                    zigzag[i] = 0;
                    i++;
                }
                zigzag[i] = rlc[j+1];
            }
        }
        return zigzag;
    }*/

    private static int[] rlcDecode2(int dc, int[] rlc) {
        int[] zigzag = new int[64];
        zigzag[0] = dc;
        for (int i = 1; i < rlcMaxLength+1; i++) {
            zigzag[i] = rlc[i-1];
        }
        return zigzag;
    }

    private static int[][] zigzagDecode(int[] zigzag) {
        int[][] X = new int[8][8];
        int index = 0;
        for (int k = 0; k <= 14; k++) {
            if (k <= 7) {
                if (k % 2 == 0) {
                    for (int i = k, j = 0; i >= 0; i--,j++,index++) {
                        X[i][j] = zigzag[index] ;
                    }
                } else {
                    for (int i = 0, j = k; i <= k; i++,j--,index++) {
                        X[i][j] = zigzag[index];
                    }
                }
            } else {
                if (k % 2 == 0) {
                    for (int i = 7, j = k - i; j <= 7; i--,j++,index++) {
                        X[i][j] = zigzag[index];
                    }
                } else {
                    for (int j = 7, i = k - j; i <= 7; i++,j--,index++) {
                        X[i][j] = zigzag[index];
                    }
                }
            }
        }
        return X;
    }

    private static int[][] quantizationDecode(int[][] Fhat, int[][] q) {
        int[][] F = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                F[i][j] = Fhat[i][j] * q[i][j];
            }
        }
        return F;
    }

    private static int[][] TYT(double[][] T, int[][] Y) {
        int[][] result = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                double r = 0;
                for (int ii = 0; ii < 8; ii++) {
                    for (int jj = 0; jj < 8; jj++) {
                        r = r + Y[ii][jj] * T[ii][i] * T[jj][j];
                    }
                }
                result[i][j] = (int)Math.round(r);
            }
        }
        return result;
    }

    private static int[][] extract(int[][] array, int blockI, int blockJ) {
        int[][] X = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                X[i][j] = array[blockI*8+i][blockJ*8+j];
            }
        }
        return X;
    }

    private static void put(int[][] rCompressedarray, int blockI, int blockJ, int[][] xDecoded) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                rCompressedarray[blockI*8+i][blockJ*8+j] = Math.max(0, Math.min(xDecoded[i][j], 255));
            }
        }
    }

    private static void save(int width, int height, List<Short> dcR, List<Short> dcG, List<Short> dcB, List<Short> rlcR, List<Short> rlcG, List<Short> rlcB) {
        try {
            OutputStream outputStream = new FileOutputStream("PNG\\out");

            System.out.println("width: "+width + " height: "+height + " dc.size: " + dcR.size() + " rlc.size: " + rlcR.size());
            writeShort(outputStream, (short)width);
            writeShort(outputStream, (short)height);
            save(outputStream, dcR, rlcR);
            save(outputStream, dcG, rlcG);
            save(outputStream, dcB, rlcB);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save(OutputStream outputStream, List<Short> dc, List<Short> rlc) throws IOException {
        writeShort(outputStream, (short)dc.size());
        for (int i = 0; i < dc.size(); i++) {
            writeShort(outputStream, dc.get(i));
            //System.out.print(dc.get(i) + " ");
        }
        System.out.println("\ndc.size " + dc.size());
        writeInt(outputStream, rlc.size());
        for (int i = 0; i < rlc.size(); i++) {
            int b = rlc.get(i);
            if (b < Byte.MIN_VALUE)
                b = Byte.MIN_VALUE;
            else if (b > Byte.MAX_VALUE)
                b = Byte.MAX_VALUE;
            outputStream.write((byte)b);
            //System.out.print(rlc.get(i) + " ");
        }
        System.out.println("\nrlc.size: " + rlc.size());
    }

    private static void load(List<Short> dcR, List<Short> dcG, List<Short> dcB, List<Short> rlcR, List<Short> rlcG, List<Short> rlcB) throws IOException {
        try {
            InputStream inputStream = new FileInputStream("PNG\\out");
            w = readShort(inputStream);
            h = readShort(inputStream);
            load(inputStream, dcR, rlcR);
            load(inputStream, dcG, rlcG);
            load(inputStream, dcB, rlcB);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load(InputStream inputStream, List<Short> dc, List<Short> rlc) throws IOException {
        short dcSize = readShort(inputStream);
        for (int i = 0; i < dcSize; i++) {
            dc.add(readShort(inputStream));
        }
        int rlcSize = readInt(inputStream);
        for (int i = 0; i < rlcSize; i++) {
            byte b = (byte)inputStream.read();
            rlc.add((short)b);
        }
    }

    private static short readShort(InputStream inputStream) throws IOException {
        short s = (short)((inputStream.read()<<8) | inputStream.read());
        return s;
    }

    private static void writeShort(OutputStream outputStream, short s) throws IOException {
        outputStream.write(s >> 8);
        outputStream.write(s);
    }

    private static int readInt(InputStream inputStream) throws IOException {
        int i = ((inputStream.read()<<24) | (inputStream.read()<<16) | (inputStream.read()<<8) | inputStream.read());
        return i;
    }

    private static void writeInt(OutputStream outputStream, int i) throws IOException {
        outputStream.write(i >> 24);
        outputStream.write(i >> 16);
        outputStream.write(i >> 8);
        outputStream.write(i);
    }
}