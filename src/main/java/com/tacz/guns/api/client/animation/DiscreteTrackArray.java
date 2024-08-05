package com.tacz.guns.api.client.animation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.Nonnull;
import java.util.*;

public class DiscreteTrackArray implements Iterable<Integer>{
    private int top = 0;
    private final ArrayList<LinkedList<Integer>> tracks;
    private int modCount = 0;

    public DiscreteTrackArray(int size) {
        tracks = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            tracks.add(null);
        }
    }

    public DiscreteTrackArray() {
        tracks = new ArrayList<>();
    }

    public int addTrackLine() {
        modCount++;
        tracks.add(null);
        return tracks.size() - 1;
    }

    public int assignNewTrack(int index) {
        if (top == Integer.MAX_VALUE) {
            throw new RuntimeException("Can't assign new track due to overflow");
        }
        modCount++;
        Optional.ofNullable(tracks.get(index)).ifPresentOrElse(
                list -> list.add(top++),
                () -> {
                    LinkedList<Integer> list = new LinkedList<>();
                    list.add(top++);
                    tracks.set(index, list);
                }
        );
        return top;
    }

    @UnmodifiableView
    public @Nonnull List<Integer> getByIndex(int index) {
        LinkedList<Integer> list = tracks.get(index);
        if(list == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(list);
        }
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return new MyIterator(modCount);
    }

    private final class MyIterator implements Iterator<Integer> {
        private final int modCount;
        private @Nullable Iterator<Integer> iterator;
        private int nextIndex;

        public MyIterator(int modCount) {
            this.modCount = modCount;
            int index = findNextNotEmptyList(-1);
            if (index != -1) {
                iterator = tracks.get(index).iterator();
                nextIndex = findNextNotEmptyList(index);
            } else {
                iterator = null;
                nextIndex = -1;
            }
        }

        @Override
        public boolean hasNext() {
            checkForModifications();
            if (iterator != null && iterator.hasNext()) {
                return true;
            }
            return nextIndex != -1;
        }

        @Override
        public Integer next() throws IllegalStateException {
            checkForModifications();
            if (iterator != null && iterator.hasNext()) {
                return iterator.next();
            }
            if (nextIndex != -1) {
                iterator = tracks.get(nextIndex).iterator();
                nextIndex = findNextNotEmptyList(nextIndex);
                return iterator.next();
            }
            throw new IllegalStateException("No more elements");
        }

        private void checkForModifications() {
            if (DiscreteTrackArray.this.modCount != modCount) {
                throw new ConcurrentModificationException("Container modified during iteration");
            }
        }

        private int findNextNotEmptyList(int index){
            int i = index + 1;
            while (i < tracks.size()) {
                LinkedList<Integer> list = tracks.get(i);
                if (list != null && !list.isEmpty()) {
                    return i;
                }
                i++;
            }
            return -1;
        }
    }
}
