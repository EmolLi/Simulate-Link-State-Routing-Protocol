package socs.network.node;

public class Link {

    RouterDescription router1;
    RouterDescription router2;
    short weight;

    /**
     *
     * @param r1 this router
     * @param r2 remote router
     */
    public Link(RouterDescription r1, RouterDescription r2) {
        router1 = r1;
        router2 = r2;
    }

    public Link(RouterDescription r1, RouterDescription r2, short weight) {
        router1 = r1;
        router2 = r2;
        this.weight = weight;
    }

    //check if the two links are equal, change this later, since we use fixed format for link
    public boolean isEqual(Link l2) {
        try {
            if ((this.router1.isEqual(l2.router1) && this.router2.isEqual(l2.router2)) || (this.router2.isEqual(l2.router1) && this.router1.isEqual(l2.router2))) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
