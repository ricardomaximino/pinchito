package es.brasatech.pinchito.model;

public enum DebitKind {
    CHECKING_PLUMBING("Revisar las tuberías", "Sexo oral (dar o recibir)"),
    SCENIC_ROUTE("Tomar la ruta escénica", "Sexo anal"),
    IMPROV_JAZZ("Sesión de jazz improvisado", "Sexo libre (¡todo vale!)"),
    PIT_STOP("Un pinchito rápido", "Un polvo rápido (sexo rápido)"),
    OFFICE_MEETING("Reunión urgente en la oficina", "Juego de rol / Fetichismo / BDSM"),
    MANUAL_TUNING("Ajuste manual de la palanca de cambios", "Masturbación o estimulación manual"),
    PIANO_DUET("Dueto de piano a cuatro manos", "Masturbación mutua"),
    FRENCH_LESSON("Clase intensiva de francés", "Besos apasionados con lengua"),
    BACK_MASSAGE("Terapia de alineación de columna", "Masaje sensual de cuerpo completo");

    private final String displayName;
    private final String description;

    DebitKind(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
