package org.athento.nxadministrative;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Created by JoséMaría on 06/04/2017.
 */

@Name("Ejemplobean")
@Scope(ScopeType.CONVERSATION)
public class Ejemplobean {

    protected String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
