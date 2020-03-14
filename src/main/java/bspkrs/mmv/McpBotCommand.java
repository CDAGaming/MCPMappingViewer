package bspkrs.mmv;

public class McpBotCommand {
    private final BotCommand command;
    private final String srgName;
    private final String newName;
    private final String comment;
    public McpBotCommand(BotCommand command, String srgName, String newName, String comment) {
        this.command = command;
        this.srgName = srgName;
        this.newName = newName;
        this.comment = comment;
    }
    public McpBotCommand(BotCommand command, String srgName, String newName) {
        this(command, srgName, newName, "");
    }

    public static BotCommand getCommand(MemberType type, boolean isForced) {
        switch (type) {
            case METHOD:
                return isForced ? BotCommand.FSM : BotCommand.SM;
            case PARAM:
                return isForced ? BotCommand.FSP : BotCommand.SP;
            default:
                return isForced ? BotCommand.FSF : BotCommand.SF;
        }
    }

    public static McpBotCommand getMcpBotCommand(MemberType type, boolean isForced, String srgName, String newName, String comment) {
        return new McpBotCommand(getCommand(type, isForced), srgName, newName, comment);
    }

    public BotCommand getCommand() {
        return command;
    }

    public String getNewName() {
        return newName;
    }

    @Override
    public String toString() {
        return String.format("!%s %s %s %s", command.toString().toLowerCase(), srgName, newName, comment);
    }

    public enum BotCommand {
        SF,
        SM,
        SP,
        FSF,
        FSM,
        FSP
    }

    public enum MemberType {
        FIELD,
        METHOD,
        PARAM
    }
}
