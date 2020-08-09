package guru.sfg.brewery.model;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class CustomerPagedList extends PageImpl<CustomerDto> implements Serializable {
	
	private static final long serialVersionUID = -5667526183846967804L;

	public CustomerPagedList(List<CustomerDto> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

	public CustomerPagedList(List<CustomerDto> content) {
		super(content);
	}

}
