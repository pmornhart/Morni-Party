package net.morni.party.board;

/** Ein Feld: Nummer auf dem Weg, Art und Mitte (relativ zum Mittelpunkt des Bretts, y = Oberseite des Felds). */
public record Field(int index, FieldType type, int x, int y, int z) {
}
