package com.adpetions.optimus.nodes;

import javax.xml.namespace.QName;

public interface QNamed {
    QName getName();

    void setName(QName qname);

    String getLocalName();

    String getNamespaceURI();

    String getPrefix();
}