package com.rli.scripts.customobjects.concurco.services;

import javax.xml.bind.annotation.XmlRegistry;

import com.rli.scripts.customobjects.concurco.data.UserProfile;

@XmlRegistry
public class ObjectFactory {
 
    public ObjectFactory() {
    }
 
    public UserProfile createUserProfile() {
        return new UserProfile();
    }
}

