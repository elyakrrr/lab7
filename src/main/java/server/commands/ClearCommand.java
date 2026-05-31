package server.commands;

import server.collection.CollectionManager;

public class ClearCommand extends BaseCommand implements CommandInvoker.ResultCapturingCommand {
    private CommandInvoker.CommandResult resultCapture;

    public ClearCommand(CollectionManager collectionManager) {
        super(collectionManager, "clear", "очистить коллекцию");
    }

    @Override
    public void execute(String[] args) {
        if (!validateArgs(args, 0, "clear")) return;

        if (resultCapture != null) {
            resultCapture.append("");
        }
    }

    @Override
    public void setResultCapture(CommandInvoker.CommandResult result) {
        this.resultCapture = result;
    }
}