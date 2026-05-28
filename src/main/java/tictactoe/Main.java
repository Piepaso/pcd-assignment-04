package tictactoe;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
        try {
            int porta = 1099; // default

            System.out.println("Avvio del registro RMI sulla porta " + porta + "...");
            Registry registry = LocateRegistry.createRegistry(porta);
            System.out.println("Registro RMI avviato con successo!");

            System.out.println("Server pronto.");

        } catch (Exception e) {
            System.err.println("Errore durante l'avvio del Server RMI:");
            e.printStackTrace();
        }
    }
}