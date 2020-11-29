package console;
import java.util.Scanner;
import static console.consoleCommand.Channel1;
import static console.consoleCommand.Channel10;
import static console.consoleCommand.Channel11;
import static console.consoleCommand.Channel12;
import static console.consoleCommand.Channel13;
import static console.consoleCommand.Channel14;
import static console.consoleCommand.Channel15;
import static console.consoleCommand.Channel16;
import static console.consoleCommand.Channel17;
import static console.consoleCommand.Channel2;
import static console.consoleCommand.Channel3;
import static console.consoleCommand.Channel4;
import static console.consoleCommand.Channel5;
import static console.consoleCommand.Channel6;
import static console.consoleCommand.Channel7;
import static console.consoleCommand.Channel8;
import static console.consoleCommand.Channel9;

/**
 *
 * @author Hades
 * @credit: kevintjuh93 (connect the string in the array)
 * @credit: Sylux123 (Rummy) (editing and fixing the scanner to make it work correctly)
 */
public class consoleScan {
    public static String commands, victim, reason, commandLC;
    public static boolean error = false;
   public static Scanner scanInput = new Scanner(System.in);
     public static String input = scanInput.nextLine();
  
    private static String joinStringFrom(String arr[], int start) {
        return joinStringFrom(arr, start, arr.length - 1);
    }

    private static String joinStringFrom(String arr[], int start, int end) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != end) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
    
    public static void run() {
        while(error == false) {
        System.out.println("Enter command here: ");
        Scanner scanInput = new Scanner(System.in);
        String input = scanInput.nextLine();
        String[] inputArray = input.split(" ");
        commandLC = inputArray[0].toLowerCase();
        // remove the check for the length of array since it was useless
        switch (commandLC) {
            case "ban":
                // still doing
                break;
            case "online":
                consoleCommand.Online();
                System.out.println(Channel1);
                System.out.println(Channel2);
                System.out.println(Channel3);
                System.out.println(Channel4);
                System.out.println(Channel5);
                System.out.println(Channel6);
                System.out.println(Channel7);
                System.out.println(Channel8);
                System.out.println(Channel9);
                System.out.println(Channel10);
                System.out.println(Channel11);
                System.out.println(Channel12);
                System.out.println(Channel13);
                System.out.println(Channel14);
                System.out.println(Channel15);
                System.out.println(Channel16);
                System.out.println(Channel17);
                
                break;
            case "servermessage":
                consoleCommand.ServerMessage(joinStringFrom(inputArray, 1));
                break;
            case "shutdown":
                System.out.println("[AlmightyMS] Server will be shutting down soon.");
                consoleCommand.shutdownServer();
                System.out.println("[AlmightyMS] Server has been shutdown!");
                break;
            case "exit":
                error = true;
                break;
        }
        }
        scanInput.close();
    }
}