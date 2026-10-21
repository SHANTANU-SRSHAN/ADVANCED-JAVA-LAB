package Calculator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;

public class Cviews extends JFrame {
    JLabel a, b, result;
    JTextField txta, txtb, txtresult;
    JButton add, sub, mul, div, perc;

    Cviews() {
        setSize(400, 300);
        setTitle("Calculator");
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        a = new JLabel("Enter A:");
        a.setBounds(20, 20, 80, 25);
        add(a);

        txta = new JTextField();
        txta.setBounds(110, 20, 200, 25);
        add(txta);

        b = new JLabel("Enter B:");
        b.setBounds(20, 60, 80, 25);
        add(b);

        txtb = new JTextField();
        txtb.setBounds(110, 60, 200, 25);
        add(txtb);

        add = new JButton("Add");
        add.setBounds(20, 110, 70, 30);
        add(add);

        sub = new JButton("Sub");
        sub.setBounds(100, 110, 70, 30);
        add(sub);

        mul = new JButton("Mul");
        mul.setBounds(20, 150, 70, 30);
        add(mul);

        div = new JButton("Div");
        div.setBounds(100, 150, 70, 30);
        add(div);

        perc = new JButton("%");
        perc.setBounds(180, 110, 70, 30);
        add(perc);

        result = new JLabel("Result:");
        result.setBounds(20, 200, 80, 25);
        add(result);

        txtresult = new JTextField();
        txtresult.setBounds(110, 200, 200, 25);
        txtresult.setEditable(false);
        add(txtresult);

        setVisible(true);
    }
}
