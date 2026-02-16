package com.humanhand.offlineassistant.voice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {
    public enum ActionType {
        OPEN_APP, CLICK, SCROLL, UNKNOWN
    }

    public static class Command {
        public ActionType action;
        public String target;
        public String direction;

        public Command(ActionType action, String target) {
            this.action = action;
            this.target = target;
        }

        public Command(ActionType action, String target, String direction) {
            this.action = action;
            this.target = target;
            this.direction = direction;
        }
    }

    public static Command parse(String text) {
        text = text.toLowerCase();
        
        if (text.startsWith("open ")) {
            return new Command(ActionType.OPEN_APP, text.substring(5).trim());
        }
        
        if (text.startsWith("click ")) {
            return new Command(ActionType.CLICK, text.substring(6).trim());
        }
        
        if (text.contains("scroll ")) {
            String direction = "down";
            if (text.contains("up")) direction = "up";
            else if (text.contains("left")) direction = "left";
            else if (text.contains("right")) direction = "right";
            return new Command(ActionType.SCROLL, null, direction);
        }
        
        return new Command(ActionType.UNKNOWN, null);
    }
}
