package study.querydsl.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
//@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item implements Persistable<String> {
	
	@Id // @GeneratedValue
	private Long id;
	
	// JPA 식별자 생성 전략이
	// @Id만 사용하여(@GeneratedValue를 사용하지않고)
	// 직접 할당이면 이처럼 구별하여 사용할 수 있다.
	// ex) @CreatedDate를 사용하여 데이터를 넣어주고
	// Persistable 인터페이스의 isNew 메소드를 구현하여
	// 기존 isNew 로직을 변경하여준다.
	
	@CreatedDate
	private LocalDateTime createdDate;

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isNew() {
		// TODO Auto-generated method stub
		return createdDate == null;
	}

}
