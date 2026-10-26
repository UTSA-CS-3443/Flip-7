import score7core.*;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println(
                        "                                   ______ \n" +
                        "                            ______|____  |\n" +
                        "                     _____ |  ____|   / / \n" +
                        "                ____|  __ \\| |__     / /  \n" +
                        "         _____ / __ \\ |__) |  __|   / /   \n" +
                        "  _____ / ____| |  ||  _  /| |____ |_/    \n" +
                        " / ____| |    | |  || | \\ \\|______|       \n" +
                        "| (___ | |    | |__||_|  \\_|              \n" +
                        " \\___ \\| |____ \\____/                     \n" +
                        " ____) |\\_____|                           \n" +
                        "|_____/                                   "
        );

        Flip7Core game = new Flip7Core();

        System.out.println("Add players");

        Scanner sc = new Scanner(System.in);
        ArrayList<String> p = new ArrayList<>();

        while(sc.hasNextLine()){
            String tp = sc.nextLine();

            // Check if the line is empty (just pressing Enter)
            if(tp.isEmpty()){
                break;
            }

            p.add(tp); // Usually you'd want to store your input too!
        }

        for(String s : p){
            game.addPlayer(s);
        }

        p.clear();

        while(!game.isGameOver()){
            System.out.println("Round:\t" + game.getRound());
            System.out.println("Whose turn is it?");
            String turn = sc.nextLine();
            Player player = game.getPlayer(turn);
            System.out.println(player.getName());
            System.out.println("Total:\t" + player.getScore() + "\tRound:\t" + player.getRoundScore() +  "\tStatus:\t" + player.getStatus());
            System.out.println("Hand:\t" + player.getHand());
            System.out.println("Actions?");
            System.out.println("(I)nput Cards\t(S)tay\t(B)ust");
            String action = sc.nextLine();
            if(action.equalsIgnoreCase("b")){
                game.bust(turn);
            }
            if(action.equalsIgnoreCase("s")){
                game.stay(turn);
            }
            if(action.equalsIgnoreCase("i")){
                while(sc.hasNextLine()){
                    String card = sc.nextLine();

                    if(card.isEmpty() || player.getStatus() != Player.Status.ACTIVE){
                        break;
                    }

                    game.addCard(turn, parseCard(card));
                }
            }
        }
    }

    static Cards parseCard(String input) {
        String s = input.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        switch (s) {
            case "X2": case "*2":                   return Cards.XTWO;
            case "+2":                              return Cards.PTWO;
            case "+4":                              return Cards.PFOUR;
            case "+6":                              return Cards.PSIX;
            case "+8":                              return Cards.PEIGHT;
            case "+10":                             return Cards.PTEN;
            case "F3": case "FLIP3": case "FLIP_3": return Cards.FLIP_THREE;
            case "SC":                              return Cards.SECOND_CHANCE;
        }

        if (s.matches("\\d{1,2}")) {
            int n = Integer.parseInt(s);
            for (Cards c : Cards.values()) {
                if (c.isNumber() && c.getValue() == n) {
                    return c;
                }
            }
        }

        try {
            return Cards.valueOf(s); // enum names: "seven", "freeze", "second chance", "flip_three"...
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown card: " + input);
        }
    }
}