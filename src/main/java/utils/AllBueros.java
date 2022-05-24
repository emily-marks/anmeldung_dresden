package utils;

import java.util.Arrays;

public enum AllBueros {
     Altstadt (0),
     Blasewitz(1),
     Cotta (7),
     Klotzsche (5),
     Leuben (2),
     Neustadt (4),
     Pieschen (3),
     Plauen (8),
     Prohlis (6);

     private final int id;

     AllBueros(int id) {
          this.id = id;
     }

     public static AllBueros getBueroById(int id) {
          return Arrays.stream(AllBueros.values()).filter(buero -> buero.id == id).findAny().get();
     }
}
