package cm.homeautomation.realbondownload.entities;

import java.math.BigDecimal;
import java.util.Currency;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class BonPosition {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    private String name;
    
    @Column(precision=8, scale=3)
    private BigDecimal quantity;
    
    @Column(precision=8, scale=2)
    private BigDecimal price;

    @Override
    public String toString() {
        return "name: " + name + "\tquantity: " + quantity + "\tprice: " + price;
    }

}