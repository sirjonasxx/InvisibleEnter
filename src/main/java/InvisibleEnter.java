import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.hashing.HashSupport;
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
        onConnect(new OnConnectionListener() {
            public void act(String s, int i, String s1) {
                HarbleAPIFetcher.fetch(s1);
            }
        });
        hashSupport = new HashSupport(this);
        ChatConsole chatConsole = new ChatConsole(hashSupport, this);

        final List<HPacket> bufferIncoming = new ArrayList<HPacket>();
        intercept(HMessage.Side.TOCLIENT, new MessageListener() {
            public void act(HMessage hMessage) {
                if (isBuffering) {
                    bufferIncoming.add(hMessage.getPacket());
                }
            }
        });

        final List<HPacket> saved = new ArrayList<HPacket>();


        hashSupport.intercept(HMessage.Side.TOSERVER, "RequestRoomData", new MessageListener() {
            public void act(HMessage hMessage) {
                if (isBuffering) {
                    isBuffering = false;
                }
            }
        });

        hashSupport.intercept(HMessage.Side.TOSERVER, "RequestRoomHeightmap", new MessageListener() {
            public void act(HMessage hMessage) {
                if (block) {
                        hMessage.setBlocked(true);
                        for (HPacket packet : saved) {
                            sendToClient(packet);
                        }
                        block = false;
                }
                else {
                    isBuffering = true;
                }
            }
        });

        chatConsole.onInput(new ChatConsole.ChatInputListener() {
            public void inputEntered(String input) {
                input = input.toLowerCase();

                if (input.equals("savelast")) {
                    isBuffering = false;
                    saved.clear();
                    saved.addAll(bufferIncoming);
                }
                else if (input.equals("block")) {
                    block = true;
                }
            }
        });
    }
}
