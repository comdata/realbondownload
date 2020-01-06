package cm.homeautomation.realbondownload;

import java.math.BigDecimal;
import java.util.Currency;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BonPosition {
    private String name;
    private BigDecimal quantity;
    private BigDecimal price;

    @Override
    public String toString() {
        return "name: " + name + "\tquantity: " + quantity + "\tprice: " + price;
    }

}