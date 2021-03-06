/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.select;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

public class NodeTraversor {
    private NodeVisitor visitor;

    public NodeTraversor(NodeVisitor visitor) {
        this.visitor = visitor;
    }

    public void traverse(Node root) {
        Node node = root;
        int depth = 0;
        while (node != null) {
            this.visitor.head(node, depth);
            if (node.childNodeSize() > 0) {
                node = node.childNode(0);
                ++depth;
                continue;
            }
            while (node.nextSibling() == null && depth > 0) {
                this.visitor.tail(node, depth);
                node = node.parentNode();
                --depth;
            }
            this.visitor.tail(node, depth);
            if (node == root) break;
            node = node.nextSibling();
        }
    }
}

