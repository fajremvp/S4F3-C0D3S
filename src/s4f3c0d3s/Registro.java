
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */

package s4f3c0d3s;

public class Registro {
    public String servico;
    public String usuario;
    public String codigos;
    public String notas;

    public Registro(String servico, String usuario, String codigos) {
        this(servico, usuario, codigos, ""); // padrão como vazio
    }

    public Registro(String servico, String usuario, String codigos, String notas) {
        this.servico = servico;
        this.usuario = usuario;
        this.codigos = codigos;
        this.notas = notas;
    }
}
