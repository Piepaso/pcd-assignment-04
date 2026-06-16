package tictactoe.service;

import java.io.Serializable;

public record SerializableMove(int position, int player) implements Serializable {
}
