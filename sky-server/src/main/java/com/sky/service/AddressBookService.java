package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {
    List<AddressBook> getList(AddressBook addressBook);


    void addAddress(AddressBook addressBook);

    void update(AddressBook addressBook);

    AddressBook getById(Long id);

    void setDefault(AddressBook addressBook);

    void deleteById(Long id);
}
