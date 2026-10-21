package Calculator;

public class Main {
    void main(){
        Cmodels cm = new Cmodels();
        Cviews cv = new Cviews();
        new Ccontroller(cm, cv);
    }
}
