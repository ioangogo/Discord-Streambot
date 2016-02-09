package ovh.gyoo.bot.commands;

import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import ovh.gyoo.bot.data.*;

import java.util.List;

public class CRemove implements Command {

    public static String name = "remove";
    private static String description = "`remove <option> <content>` : Remove data from the bot";

    @Override
    public void execute(MessageReceivedEvent e, String content) {
        MessageItem message = new MessageItem(e.getTextChannel().getId(), MessageItem.Type.GUILD);
        if (!isAllowed(e.getGuild().getId(), e.getAuthor().getId()))
            message.setMessage(new MessageBuilder().appendString("You are not allowed to use this command").build());
        else if (getPermissionLevel(e.getGuild().getId(), e.getAuthor().getId()) == Permissions.QUEUE) {
            ServerList.getInstance().getServer(e.getGuild().getId()).queueCommand(e.getAuthor().getUsername(), "`!streambot remove " + content + "`");
            message.setMessage(new MessageBuilder()
                    .appendString("Your request will be treated by a manager soon! (type `!streambot list manager` to check the managers list)")
                    .build());
        } else {
            String option = content.substring(0, content.indexOf(" "));
            String[] contents = content.substring(content.indexOf(" ")).split("\\|");
            switch (option) {
                case "game":
                    message.setMessage(removeGame(e.getGuild().getId(), contents));
                    break;
                case "channel":
                    message.setMessage(removeChannel(e.getGuild().getId(), contents));
                    break;
                case "tag":
                    message.setMessage(removeTag(e.getGuild().getId(), contents));
                    break;
                case "manager":
                    message.setMessage(removeManagers(e.getGuild().getId(),e.getAuthor().getId(), e.getMessage().getMentionedUsers()));
                    break;
                default:
                    message.setMessage(new MessageBuilder()
                            .appendString("Unknown command")
                            .build());
                    break;
            }
        }
        DiscordInstance.getInstance().addToQueue(message);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isAllowed(String serverID, String authorID) {
        LocalServer ls = ServerList.getInstance().getServer(serverID);
        return ls.getManagers().contains(authorID) || getPermissionLevel(serverID, authorID) < Permissions.FORBID;
    }

    private int getPermissionLevel(String serverID, String authorID) {
        int res = Permissions.FORBID;
        LocalServer ls = ServerList.getInstance().getServer(serverID);
        if(ls.getManagers().contains(authorID)) return Permissions.USE;
        for (Role r : DiscordInstance.getInstance().getDiscord().getGuildById(serverID).getRolesForUser(DiscordInstance.getInstance().getDiscord().getUserById(authorID))) {
            res = Math.min(res, ls.getPermissionsMap().get(name).getPerms().getOrDefault(r.getId(), Permissions.FORBID));
        }
        res = Math.min(res,ls.getPermissionsMap().get(name).getPerms().getOrDefault("everyone", Permissions.FORBID));
        return res;
    }

    private Message removeGame(String serverId, String[] game){
        MessageBuilder mb = new MessageBuilder();
        for(String s : game){
            s = s.trim();
            boolean res = ServerList.getInstance().getServer(serverId).removeGame(s);
            if(res)
                mb.appendString("Game " + s + " removed from the game list\n");
            else
                mb.appendString("Game " + s + " is not in the game list\n");
        }
        return mb.build();
    }

    private Message removeChannel(String serverId, String[] channels){
        MessageBuilder mb = new MessageBuilder();
        for(String s : channels){
            s = s.trim();
            boolean res = ServerList.getInstance().getServer(serverId).removeUser(s.replaceAll("`", "").toLowerCase());
            if(res)
                mb.appendString("Channel " + s + " removed from the channels list\n");
            else
                mb.appendString("Channel " + s + " is not in the channels list\n");
        }
        return mb.build();
    }

    private Message removeTag(String serverId, String[] tags){
        MessageBuilder mb = new MessageBuilder();
        for(String s : tags){
            s = s.trim();
            boolean res = ServerList.getInstance().getServer(serverId).removeTag(s);
            if(res)
                mb.appendString("Tag " + s + " removed from the tags list\n");
            else
                mb.appendString("Tag " + s + " is not in the tags list\n");
        }
        return mb.build();
    }

    private Message removeManagers(String serverId, String userId, List<User> users){
        MessageBuilder builder = new MessageBuilder();
        for(User u : users){
            if(ServerList.getInstance().getServer(serverId).getManagers().size() == 1){
                builder.appendString("Cannot remove managers : There must be at least one manager per server\n");
                break;
            }
            if(userId.equals(u.getId())){
                builder.appendString("You cannot remove yourself !\n");
                continue;
            }
            boolean res = ServerList.getInstance().getServer(serverId).removeManager(u.getId());
            if(res)
                builder.appendString("User " + u.getUsername() + " removed from the managers list\n");
            else
                builder.appendString("User " + u.getUsername() + " is not in the managers list\n");
        }
        return builder.build();
    }
}