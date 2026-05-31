package server.commands;

import server.collection.CollectionManager;
import shared.model.Person;

public class RemoveGreaterCommand extends BaseCommand implements CommandInvoker.ElementCommand {

    public RemoveGreaterCommand(CollectionManager collectionManager) {
        super(collectionManager, "remove_greater", "удалить элементы, превышающие заданный");
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