import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.harble.ChatConsole;
import gearth.extensions.extra.harble.ChatInputListener;
import gearth.extensions.extra.harble.HashSupport;
import gearth.misc.harble_api.HarbleAPIFetcher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeunez on 1/12/2018.
 */

@ExtensionInfo(
        Title = "Invisible Entrance",
        Description = "Makes you invisible for entrance wired",
        Version = "1.0",
        Author = "sirjonasxx"
)
public class InvisibleEnter extends Extension {

    public static void main(String[] args) {
        new InvisibleEnter(args).run();
    }
    public InvisibleEnter(String[] args) {
        super(args);
    }

    private HashSupport hashSupport;
    private volatile boolean isBuffering = false;
    private volatile boolean block = false;

    @Override
    protected void initExtension() {
        hashSupport = new HashSupport(this);

        String initmsg =
                "Welcome to InvisibleEnter, a tool you can use to bypass the entrance wired of a room. To use this extension, use the following guidelines.\n" +
                        "--------------------------------------\n" +
                        "\n" +
                        "* After entering a room where you want to block the entrance wired, type \":savelast\"\n" +
                        "\n" +
                        "* Now re-enter that room, but before you do, type \":block\"";

        final ChatConsole chatConsole = new ChatConsole(hashSupport, this, initmsg);

        final List<HPacket> bufferIncoming = new ArrayList<HPacket>();
        intercept(HMessage.Direction.TOCLIENT, new MessageListener() {
            public void act(HMessage hMessage) {
                if (isBuffering) {
                    bufferIncoming.add(hMessage.getPacket());
                }
            }
        });

        final List<HPacket> saved = new ArrayList<HPacket>();


        hashSupport.intercept(HMessage.Direction.TOSERVER, "RequestRoomData", new MessageListener() {
            public void act(HMessage hMessage) {
                if (isBuffering) {
                    isBuffering = false;
                }
            }
        });

        hashSupport.intercept(HMessage.Direction.TOSERVER, "RequestRoomHeightmap", new MessageListener() {
            public void act(HMessage hMessage) {
                if (block) {
                        hMessage.setBlocked(true);
                        for (HPacket packet : saved) {
                            sendToClient(packet);
                        }
                        block = false;
                        chatConsole.writeOutput("Bypassed the room entrance wired", false);
                }
                else {
                    bufferIncoming.clear();
                    isBuffering = true;
                }
            }
        });

        chatConsole.onInput(new ChatInputListener() {
            public void inputEntered(String input) {
                input = input.toLowerCase();

                if (input.equals(":savelast")) {
                    isBuffering = false;
                    saved.clear();
                    saved.addAll(bufferIncoming);
                    chatConsole.writeOutput("Saving " + saved.size() + " packets", false);

                }
                else if (input.equals(":block")) {
                    block = true;
                }
            }
        });
    }
}
