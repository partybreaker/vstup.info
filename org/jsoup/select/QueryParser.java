/*
 * Decompiled with CFR 0_114.
 */
package org.jsoup.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;
import org.jsoup.select.CombiningEvaluator;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;
import org.jsoup.select.StructuralEvaluator;

class QueryParser {
    private static final String[] combinators = new String[]{",", ">", "+", "~", " "};
    private static final String[] AttributeEvals = new String[]{"=", "!=", "^=", "$=", "*=", "~="};
    private TokenQueue tq;
    private String query;
    private List<Evaluator> evals = new ArrayList<Evaluator>();
    private static final Pattern NTH_AB = Pattern.compile("((\\+|-)?(\\d+)?)n(\\s*(\\+|-)?\\s*\\d+)?", 2);
    private static final Pattern NTH_B = Pattern.compile("(\\+|-)?(\\d+)");

    private QueryParser(String query) {
        this.query = query;
        this.tq = new TokenQueue(query);
    }

    public static Evaluator parse(String query) {
        QueryParser p = new QueryParser(query);
        return p.parse();
    }

    Evaluator parse() {
        this.tq.consumeWhitespace();
        if (this.tq.matchesAny(combinators)) {
            this.evals.add(new StructuralEvaluator.Root());
            this.combinator(this.tq.consume());
        } else {
            this.findElements();
        }
        while (!this.tq.isEmpty()) {
            boolean seenWhite = this.tq.consumeWhitespace();
            if (this.tq.matchesAny(combinators)) {
                this.combinator(this.tq.consume());
                continue;
            }
            if (seenWhite) {
                this.combinator(' ');
                continue;
            }
            this.findElements();
        }
        if (this.evals.size() == 1) {
            return this.evals.get(0);
        }
        return new CombiningEvaluator.And(this.evals);
    }

    private void combinator(char combinator) {
        Evaluator rootEval;
        Evaluator currentEval;
        this.tq.consumeWhitespace();
        String subQuery = this.consumeSubQuery();
        Evaluator newEval = QueryParser.parse(subQuery);
        boolean replaceRightMost = false;
        if (this.evals.size() == 1) {
            currentEval = this.evals.get(0);
            rootEval = currentEval;
            if (rootEval instanceof CombiningEvaluator.Or && combinator != ',') {
                currentEval = ((CombiningEvaluator.Or)currentEval).rightMostEvaluator();
                replaceRightMost = true;
            }
        } else {
            rootEval = currentEval = new CombiningEvaluator.And(this.evals);
        }
        this.evals.clear();
        if (combinator == '>') {
            currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.ImmediateParent(currentEval));
        } else if (combinator == ' ') {
            currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.Parent(currentEval));
        } else if (combinator == '+') {
            currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.ImmediatePreviousSibling(currentEval));
        } else if (combinator == '~') {
            currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.PreviousSibling(currentEval));
        } else if (combinator == ',') {
            CombiningEvaluator.Or or;
            if (currentEval instanceof CombiningEvaluator.Or) {
                or = (CombiningEvaluator.Or)currentEval;
                or.add(newEval);
            } else {
                or = new CombiningEvaluator.Or();
                or.add(currentEval);
                or.add(newEval);
            }
            currentEval = or;
        } else {
            throw new Selector.SelectorParseException("Unknown combinator: " + combinator, new Object[0]);
        }
        if (replaceRightMost) {
            ((CombiningEvaluator.Or)rootEval).replaceRightMostEvaluator(currentEval);
        } else {
            rootEval = currentEval;
        }
        this.evals.add(rootEval);
    }

    private String consumeSubQuery() {
        StringBuilder sq = new StringBuilder();
        while (!this.tq.isEmpty()) {
            if (this.tq.matches("(")) {
                sq.append("(").append(this.tq.chompBalanced('(', ')')).append(")");
                continue;
            }
            if (this.tq.matches("[")) {
                sq.append("[").append(this.tq.chompBalanced('[', ']')).append("]");
                continue;
            }
            if (this.tq.matchesAny(combinators)) break;
            sq.append(this.tq.consume());
        }
        return sq.toString();
    }

    private void findElements() {
        if (this.tq.matchChomp("#")) {
            this.byId();
        } else if (this.tq.matchChomp(".")) {
            this.byClass();
        } else if (this.tq.matchesWord()) {
            this.byTag();
        } else if (this.tq.matches("[")) {
            this.byAttribute();
        } else if (this.tq.matchChomp("*")) {
            this.allElements();
        } else if (this.tq.matchChomp(":lt(")) {
            this.indexLessThan();
        } else if (this.tq.matchChomp(":gt(")) {
            this.indexGreaterThan();
        } else if (this.tq.matchChomp(":eq(")) {
            this.indexEquals();
        } else if (this.tq.matches(":has(")) {
            this.has();
        } else if (this.tq.matches(":contains(")) {
            this.contains(false);
        } else if (this.tq.matches(":containsOwn(")) {
            this.contains(true);
        } else if (this.tq.matches(":matches(")) {
            this.matches(false);
        } else if (this.tq.matches(":matchesOwn(")) {
            this.matches(true);
        } else if (this.tq.matches(":not(")) {
            this.not();
        } else if (this.tq.matchChomp(":nth-child(")) {
            this.cssNthChild(false, false);
        } else if (this.tq.matchChomp(":nth-last-child(")) {
            this.cssNthChild(true, false);
        } else if (this.tq.matchChomp(":nth-of-type(")) {
            this.cssNthChild(false, true);
        } else if (this.tq.matchChomp(":nth-last-of-type(")) {
            this.cssNthChild(true, true);
        } else if (this.tq.matchChomp(":first-child")) {
            this.evals.add(new Evaluator.IsFirstChild());
        } else if (this.tq.matchChomp(":last-child")) {
            this.evals.add(new Evaluator.IsLastChild());
        } else if (this.tq.matchChomp(":first-of-type")) {
            this.evals.add(new Evaluator.IsFirstOfType());
        } else if (this.tq.matchChomp(":last-of-type")) {
            this.evals.add(new Evaluator.IsLastOfType());
        } else if (this.tq.matchChomp(":only-child")) {
            this.evals.add(new Evaluator.IsOnlyChild());
        } else if (this.tq.matchChomp(":only-of-type")) {
            this.evals.add(new Evaluator.IsOnlyOfType());
        } else if (this.tq.matchChomp(":empty")) {
            this.evals.add(new Evaluator.IsEmpty());
        } else if (this.tq.matchChomp(":root")) {
            this.evals.add(new Evaluator.IsRoot());
        } else {
            throw new Selector.SelectorParseException("Could not parse query '%s': unexpected token at '%s'", this.query, this.tq.remainder());
        }
    }

    private void byId() {
        String id = this.tq.consumeCssIdentifier();
        Validate.notEmpty(id);
        this.evals.add(new Evaluator.Id(id));
    }

    private void byClass() {
        String className = this.tq.consumeCssIdentifier();
        Validate.notEmpty(className);
        this.evals.add(new Evaluator.Class(className.trim().toLowerCase()));
    }

    private void byTag() {
        String tagName = this.tq.consumeElementSelector();
        Validate.notEmpty(tagName);
        if (tagName.contains("|")) {
            tagName = tagName.replace("|", ":");
        }
        this.evals.add(new Evaluator.Tag(tagName.trim().toLowerCase()));
    }

    private void byAttribute() {
        TokenQueue cq = new TokenQueue(this.tq.chompBalanced('[', ']'));
        String key = cq.consumeToAny(AttributeEvals);
        Validate.notEmpty(key);
        cq.consumeWhitespace();
        if (cq.isEmpty()) {
            if (key.startsWith("^")) {
                this.evals.add(new Evaluator.AttributeStarting(key.substring(1)));
            } else {
                this.evals.add(new Evaluator.Attribute(key));
            }
        } else if (cq.matchChomp("=")) {
            this.evals.add(new Evaluator.AttributeWithValue(key, cq.remainder()));
        } else if (cq.matchChomp("!=")) {
            this.evals.add(new Evaluator.AttributeWithValueNot(key, cq.remainder()));
        } else if (cq.matchChomp("^=")) {
            this.evals.add(new Evaluator.AttributeWithValueStarting(key, cq.remainder()));
        } else if (cq.matchChomp("$=")) {
            this.evals.add(new Evaluator.AttributeWithValueEnding(key, cq.remainder()));
        } else if (cq.matchChomp("*=")) {
            this.evals.add(new Evaluator.AttributeWithValueContaining(key, cq.remainder()));
        } else if (cq.matchChomp("~=")) {
            this.evals.add(new Evaluator.AttributeWithValueMatching(key, Pattern.compile(cq.remainder())));
        } else {
            throw new Selector.SelectorParseException("Could not parse attribute query '%s': unexpected token at '%s'", this.query, cq.remainder());
        }
    }

    private void allElements() {
        this.evals.add(new Evaluator.AllElements());
    }

    private void indexLessThan() {
        this.evals.add(new Evaluator.IndexLessThan(this.consumeIndex()));
    }

    private void indexGreaterThan() {
        this.evals.add(new Evaluator.IndexGreaterThan(this.consumeIndex()));
    }

    private void indexEquals() {
        this.evals.add(new Evaluator.IndexEquals(this.consumeIndex()));
    }

    private void cssNthChild(boolean backwards, boolean ofType) {
        int b;
        int a;
        String argS = this.tq.chompTo(")").trim().toLowerCase();
        Matcher mAB = NTH_AB.matcher(argS);
        Matcher mB = NTH_B.matcher(argS);
        if ("odd".equals(argS)) {
            a = 2;
            b = 1;
        } else if ("even".equals(argS)) {
            a = 2;
            b = 0;
        } else if (mAB.matches()) {
            a = mAB.group(3) != null ? Integer.parseInt(mAB.group(1).replaceFirst("^\\+", "")) : 1;
            b = mAB.group(4) != null ? Integer.parseInt(mAB.group(4).replaceFirst("^\\+", "")) : 0;
        } else if (mB.matches()) {
            a = 0;
            b = Integer.parseInt(mB.group().replaceFirst("^\\+", ""));
        } else {
            throw new Selector.SelectorParseException("Could not parse nth-index '%s': unexpected format", argS);
        }
        if (ofType) {
            if (backwards) {
                this.evals.add(new Evaluator.IsNthLastOfType(a, b));
            } else {
                this.evals.add(new Evaluator.IsNthOfType(a, b));
            }
        } else if (backwards) {
            this.evals.add(new Evaluator.IsNthLastChild(a, b));
        } else {
            this.evals.add(new Evaluator.IsNthChild(a, b));
        }
    }

    private int consumeIndex() {
        String indexS = this.tq.chompTo(")").trim();
        Validate.isTrue(StringUtil.isNumeric(indexS), "Index must be numeric");
        return Integer.parseInt(indexS);
    }

    private void has() {
        this.tq.consume(":has");
        String subQuery = this.tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":has(el) subselect must not be empty");
        this.evals.add(new StructuralEvaluator.Has(QueryParser.parse(subQuery)));
    }

    private void contains(boolean own) {
        this.tq.consume(own ? ":containsOwn" : ":contains");
        String searchText = TokenQueue.unescape(this.tq.chompBalanced('(', ')'));
        Validate.notEmpty(searchText, ":contains(text) query must not be empty");
        if (own) {
            this.evals.add(new Evaluator.ContainsOwnText(searchText));
        } else {
            this.evals.add(new Evaluator.ContainsText(searchText));
        }
    }

    private void matches(boolean own) {
        this.tq.consume(own ? ":matchesOwn" : ":matches");
        String regex = this.tq.chompBalanced('(', ')');
        Validate.notEmpty(regex, ":matches(regex) query must not be empty");
        if (own) {
            this.evals.add(new Evaluator.MatchesOwn(Pattern.compile(regex)));
        } else {
            this.evals.add(new Evaluator.Matches(Pattern.compile(regex)));
        }
    }

    private void not() {
        this.tq.consume(":not");
        String subQuery = this.tq.chompBalanced('(', ')');
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty");
        this.evals.add(new StructuralEvaluator.Not(QueryParser.parse(subQuery)));
    }
}

