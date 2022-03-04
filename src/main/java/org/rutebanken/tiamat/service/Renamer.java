package org.rutebanken.tiamat.service;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class Renamer {

    /**
     * Apply various rules to a name to make it compliant with Modalis recommendations
     *
     * @param name original name
     * @return modified name if so, null otherwise.
     */
    public String renameIfNeeded(String name) {

        String originName = name;

        name = WordUtils.capitalizeFully(name);

        name = name.replace("College", "Collège");
        name = name.replace("Collége", "Collège");

        name = name.replace(" A ", " à ");
        name = name.replace(" À ", " à ");

        name = name.replace("Z.A", "ZA");
        name = name.replace("ZAE.", "ZAE");
        name = name.replace("Z.I.", "ZI");

        name = name.replace(" De ", " de ");
        name = name.replace(" Le ", " le ");
        name = name.replace(" La ", " la ");
        name = name.replace(" Du ", " du ");
        name = name.replace(" Des ", " des ");
        name = name.replace("- la ", "- La ");

        name = name.replace("Rte ", "Route ");
        name = name.replace("Lot. ", "Lotissement ");
        name = name.replace("Imp. ", "Impasse ");
        name = name.replace("Av. ", "Avenue ");
        name = name.replace("Pl. ", "Place ");
        name = name.replace("Ch. ", "Chemin ");
        name = name.replace("St ", "Saint ");
        name = name.replace("St-", "Saint-");


        name = name.replace(" D'", " d'");
        name = name.replace(" L'", " l'");
        name = name.replace("-L'", "-l'");
        name = name.replace(" D ", " d'");
        name = name.replace("-D'", "-d'");

        name = name.replace("Quatre", "4");

        name = name.replace("Sncf", "SNCF");
        name = name.replace("Inra", "INRA");
        name = name.replace("Irsa", "IRSA");
        name = name.replace("Capc", "CAPC");
        name = name.replace(" Lpi", " LPI");
        name = name.replace("Ddass", "DDASS");
        name = name.replace("Aft", "AFT");
        name = name.replace(" Lep", " LEP");
        name = name.replace(" Cfa", " CFA");
        name = name.replace(" Hlm", " HLM");
        name = name.replace(" Zae", " ZAE");
        name = name.replace("Enap", " ENAP");
        name = name.replace("Edf", "EDF");
        name = name.replace("Min", "MIN");


        if (!originName.equals(name)) {
            return name;
        } else {
            return null;
        }
    }
}
