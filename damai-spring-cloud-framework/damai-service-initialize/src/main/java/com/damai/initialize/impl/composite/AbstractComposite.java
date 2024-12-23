package com.damai.initialize.impl.composite;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class AbstractComposite<T> {

    protected List<AbstractComposite<T>> list = new ArrayList<>();

    protected abstract void execute(T param);

    protected abstract String type();

    public abstract Integer executeParentOrder();

    public abstract Integer executeTier();

    public abstract Integer executeOrder();

    public void add(AbstractComposite<T> abstractComposite) {
        list.add(abstractComposite);
    }

    public void allExecute(T param) {
        Queue<AbstractComposite<T>> queue = new LinkedList<>();

        queue.add(this);

        while(!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                AbstractComposite<T> current = queue.poll();
                assert current != null;
                current.execute(param);
                queue.addAll(current.list);
            }
        }
    }
}
