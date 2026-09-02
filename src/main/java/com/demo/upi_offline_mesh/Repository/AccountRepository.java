package com.demo.upi_offline_mesh.Repository;

import com.demo.upi_offline_mesh.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository  extends JpaRepository<Account, String> {

}
