
/*
 * S4F3-C0D3S - Recovery Codes Manager
 * Developed by Fajre
 * Originally distributed exclusively via the author's GitHub: https://github.com/fajremvp/S4F3-C0D3S
 * Licensed under the MIT License
 */

package s4f3c0d3s;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Enumeration;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public class LookAndFeelUtils {
   
     //Carrega a fonte TTF de dentro do JAR (resources/segoeui.ttf) e aplica como padrão a todos os componentes Swing.
	
    public static void loadAndSetDefaultFont(String resourcePath, int style, float size) {
        try (InputStream is = LookAndFeelUtils.class.getResourceAsStream(resourcePath)) {
            Font base = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(style, size);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(base);
            setDefaultFont(base);
        } catch (Exception ex) {
            // Se falhar, tenta usar diretamente pelo nome
            setDefaultFont(new Font("Segoe UI", style, Math.round(size)));
            ex.printStackTrace();
        }
    }

    public static void setDefaultFont(Font f) {
        FontUIResource fr = new FontUIResource(f);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object val = UIManager.get(key);
            if (val instanceof FontUIResource) {
                UIManager.put(key, fr);
            }
        }
    }
}
