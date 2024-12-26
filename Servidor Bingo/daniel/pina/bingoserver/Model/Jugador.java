package daniel.pina.bingoserver.Model;

public class Jugador {
    private String nombre;
    private Integer lineas;
    private Integer bingos;

    public Jugador(String nombre){
        this.nombre = nombre;
    }

    public void setLineas(int lineas){
        this.lineas = lineas;
    }

    public void setBingos(int bingos){
        this.bingos = bingos;
    }

    public String getNombre(){
        return this.nombre;
    }

    public Integer getLineas(){
        return this.lineas;
    }

    public Integer getBingos(){
        return this.bingos;
    }
}
