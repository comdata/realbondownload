package cm.homeautomation.realbondownload.entities;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Bon {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique=true)
	Date bonDate;
	
	@Column(precision=8, scale=2)
	BigDecimal price;
	BigDecimal payback;
	BigDecimal paybackExtra;
	
	@OneToMany(cascade=CascadeType.PERSIST)
	List<BonPosition> bonPositions;
}
