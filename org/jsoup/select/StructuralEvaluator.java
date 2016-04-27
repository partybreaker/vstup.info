/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

abstract class StructuralEvaluator
extends Evaluator {
    Evaluator evaluator;

    StructuralEvaluator() {
    }

    static class ImmediatePreviousSibling
    extends StructuralEvaluator {
        public ImmediatePreviousSibling(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element element) {
            if (root == element) {
                return false;
            }
            Element prev = element.previousElementSibling();
            return prev != null && this.evaluator.matches(root, prev);
        }

        public String toString() {
            return String.format(":prev%s", this.evaluator);
        }
    }

    static class PreviousSibling
    extends StructuralEvaluator {
        public PreviousSibling(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element element) {
            if (root == element) {
                return false;
            }
            for (Element prev = element.previousElementSibling(); prev != null; prev = prev.previousElementSibling()) {
                if (!this.evaluator.matches(root, prev)) continue;
                return true;
            }
            return false;
        }

        public String toString() {
            return String.format(":prev*%s", this.evaluator);
        }
    }

    static class ImmediateParent
    extends StructuralEvaluator {
        public ImmediateParent(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element element) {
            if (root == element) {
                return false;
            }
            Element parent = element.parent();
            return parent != null && this.evaluator.matches(root, parent);
        }

        public String toString() {
            return String.format(":ImmediateParent%s", this.evaluator);
        }
    }

    static class Parent
    extends StructuralEvaluator {
        public Parent(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element element) {
            if (root == element) {
                return false;
            }
            for (Element parent = element.parent(); parent != root; parent = parent.parent()) {
                if (!this.evaluator.matches(root, parent)) continue;
                return true;
            }
            return false;
        }

        public String toString() {
            return String.format(":parent%s", this.evaluator);
        }
    }

    static class Not
    extends StructuralEvaluator {
        public Not(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element node) {
            return !this.evaluator.matches(root, node);
        }

        public String toString() {
            return String.format(":not%s", this.evaluator);
        }
    }

    static class Has
    extends StructuralEvaluator {
        public Has(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element element) {
            for (Element e : element.getAllElements()) {
                if (e == element || !this.evaluator.matches(root, e)) continue;
                return true;
            }
            return false;
        }

        public String toString() {
            return String.format(":has(%s)", this.evaluator);
        }
    }

    static class Root
    extends Evaluator {
        Root() {
        }

        public boolean matches(Element root, Element element) {
            return root == element;
        }
    }

}

