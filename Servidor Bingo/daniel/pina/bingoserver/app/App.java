package daniel.pina.bingoserver.app;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class App {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Servidor de Bingo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 200);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        JLabel statusLabel = new JLabel("Servidor detenido", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(statusLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton startButton = new JButton("Iniciar Servidor");
        JButton playButton = new JButton("Comenzar Partida");
        JButton stopButton = new JButton("Terminar Partida");
        playButton.setEnabled(false);
        stopButton.setEnabled(false); 
        buttonPanel.add(startButton);
        buttonPanel.add(playButton);
        buttonPanel.add(stopButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                new Thread(() -> {
                    BingoServer.startServer(); 
                }).start();
                
                startButton.setEnabled(false);
                playButton.setEnabled(true);
                statusLabel.setText("Servidor iniciado en " + BingoServer.obtenerIPWiFi());
                
            }
        });

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                
                if (!BingoServer.getPartidaComenzada()) {
                    BingoServer.comenzarPartida();
                    BingoServer.broadcast("PARTIDA COMENZADA");
                    statusLabel.setText("Partida en curso...");
                    playButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    System.out.println("Partida iniciada.");
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (BingoServer.getPartidaComenzada()) {
                    BingoServer.finalizarPartida();
                    statusLabel.setText("Partida terminada.");
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    System.out.println("Partida terminada.");
                }
            }
        });

        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);

    }
}

