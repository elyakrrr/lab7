package server.commands;

import server.collection.CollectionManager;
import shared.model.Person;

public class RemoveLowerCommand extends BaseCommand implements CommandInvoker.ElementCommand {

    public RemoveLowerCommand(CollectionManager collectionManager) {
        super(collectionManager, "remove_lower", "удалить элементы, меньшие чем заданный");
    }

    @Override
    public void execute(String[] args) {
        // Логика полностью в RequestProcessorThread.handleWriteCommand
    }

    @Override
    public void executeWithElement(String[] args, Person person) {
        // Логика полностью в RequestProcessorThread.handleWriteCommand
    }
}