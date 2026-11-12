package pl.tracer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mała konfiguracja w MSSQL: nazwa wyświetlana i kolor serwisu na diagramie. */
@Entity
@Table(name = "service_config")
public class ServiceConfig {
    @Id private String name;
    private String displayName;
    private String color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
