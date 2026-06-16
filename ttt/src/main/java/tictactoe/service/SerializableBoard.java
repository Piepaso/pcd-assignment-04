package tictactoe.service;

import java.io.Serializable;
import java.util.List;

public record SerializableBoard(List<Integer> cells) implements Serializable {
}
