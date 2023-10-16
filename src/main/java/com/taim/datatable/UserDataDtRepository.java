package com.taim.datatable;

import com.taim.conduire.domain.UserData;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDataDtRepository extends DataTablesRepository<UserData, Integer> {
   
}	