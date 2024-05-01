package Client;

import DrawingObject.drawingPanelElements.DeleteAll;
import DrawingObject.Shape.DrawingShape;
import DrawingObject.InitWindow.PopupWindow;
import DrawingObject.drawingPanelElements.ExistingCanvas;
import DrawingObject.drawingPanelElements.SavedCanvas;
import DrawingObject.drawingPanelElements.TextOnBoard;
import ShakeHands.ChatWindow.Message;
import ShakeHands.CloseMessage;
import ShakeHands.InitialCommunication;
import ShakeHands.Notice;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import static ShakeHands.Util.ConnectUtil.*;

public class ClientSideHandler {
    private final Socket socket;
    private final Logger logger;
    private final WhiteBoardClient client;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private WhiteBoardGUI wb;

    public ClientSideHandler(Socket socket, Logger logger, WhiteBoardClient client) {
        this.socket = socket;
        this.logger = logger;
        this.client = client;
    }

    public void startCommunication(InitialCommunication initialCommunication) {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            output.writeObject(initialCommunication);
            output.flush();

            int statusCode =  waitTilServerReply();

            actionOnStatusCode(statusCode, initialCommunication);
            // Start a thread to listen for server updates
            new Thread(this::listenForServerUpdates).start();
        } catch (IOException e) {
            logger.severe("Error initializing communication streams: " + e.getMessage());
        }
    }

    private void actionOnStatusCode(int statusCode, InitialCommunication initialCommunication) {
        logger.info("received status code: "+ statusCode);
        if (statusCode == AcceptCreate) {
            wb.setAdmin(true);
            wb.toggleFileButtonVisibility();
            wb.setUserName(initialCommunication.getUsername());
            return;
        }else if (statusCode == AcceptJoin) {
            wb.setUserName(initialCommunication.getUsername());
            wb.setAdmin(false);
            return;
        }
        printErrorStatusInfo(statusCode, logger);
        SwingUtilities.invokeLater(() -> {
            PopupWindow popup = new PopupWindow(message, () -> {
                closeConnection();
                System.exit(1);
            });
            popup.adminClose();
        });
    }

    private int waitTilServerReply() throws IOException {
        while (true) {
            try {
                return (int) input.readObject();
            } catch (ClassNotFoundException | IOException e) {
                logger.severe("Error reading object from server: " + e.getMessage());
                throw new IOException("Class not found: " + e.getMessage()); // Rethrow as IOException
            }
        }
    }

    private void listenForServerUpdates() {
        try {
            Object update;
            while ((update = input.readObject()) != null) {
                handleServerUpdate(update);
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Error reading from server from listener: " + e.getMessage());
            closeConnection();
        }
    }

    private void handleServerUpdate(Object update) {
        if (update instanceof DrawingShape) {  // Assuming 'Shape' is a class for graphical objects
            DrawingShape shape = (DrawingShape) update;
            SwingUtilities.invokeLater(() -> {
                wb.updateDrawing(shape);
            });
        } else if (update instanceof DeleteAll) {
            System.out.println("receive delete");
            wb.deleteAll();
        } else if (update instanceof TextOnBoard){
            TextOnBoard textOnBoard = (TextOnBoard) update;
            SwingUtilities.invokeLater(() -> {
                wb.updateTextFields(textOnBoard);
            });
        } else if (update instanceof CloseMessage) {
            SwingUtilities.invokeLater(() -> {
                PopupWindow popup = new PopupWindow("Admin has closed the whiteboard, logging you out", () -> {
                    closeConnection();
                    System.exit(1);
                });
                popup.adminClose();
            });
        } else if (update instanceof Notice) {
            Notice notice = (Notice) update;
            System.out.println("received notice: " + notice.getUsername() + notice.isLeaving());

            SwingUtilities.invokeLater(() -> {
                wb.showNotice(notice);
            });
        } else if (update instanceof Message) {
            Message msg = (Message) update;
            wb.updateChatWindow(msg);
        } else if (update instanceof ExistingCanvas) {
            wb.showChoice((ExistingCanvas)update);
        } else if (update instanceof SavedCanvas) {
            System.out.println("received");
            wb.syncBufferedImage((SavedCanvas)update);
        }
    }

    public void sendUpdateToServer(Object update) {
        try {
            if (output != null) {
                output.reset();
                output.writeObject(update);
                output.flush();
            }
        } catch (IOException e) {
            closeConnection();
        }
    }

    public void startWhiteBoard() {
        wb = new WhiteBoardGUI(this);
        wb.display();
    }

    private void closeConnection() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.severe("Error closing network resources: " + e.getMessage());
        }
    }

}
