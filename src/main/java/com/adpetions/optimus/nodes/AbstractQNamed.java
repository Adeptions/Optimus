package com.adpetions.optimus.nodes;

import javax.xml.namespace.QName;

abstract class AbstractQNamed implements QNamed {
    protected QName qname;

    @Override
    public QName getName() {
        return qname;
    }

    @Override
    public void setName(QName qname) {
        this.qname = qname;
    }

    @Override
    public String getLocalName() {
        return qname.getLocalPart();
    }

    @Override
    public String getNamespaceURI() {
        return qname.getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        return qname.getPrefix();
    }

}
