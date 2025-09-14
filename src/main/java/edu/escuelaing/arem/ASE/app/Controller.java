package edu.escuelaing.arem.ASE.app;


import edu.escuelaing.arem.ASE.app.Annotations.*;
@RestController
public class Controller {

    @GetMapping("/greeting")
    public String greet(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }

    @GetMapping("/string")
    public String hi(){
        return "Greetings from Spring Boot!";
    }

    @GetMapping("/pi")
    public String pi(){return String.valueOf(Math.PI);}
}

