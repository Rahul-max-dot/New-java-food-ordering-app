
let foods = [];
let cart = {};

async function loadFoods() {
    const response = await fetch("/api/foods");
    foods = await response.json();

    const menu = document.getElementById("menu");
    menu.innerHTML = "";

    foods.forEach(food => {
        const div = document.createElement("div");
        div.className = "food";

        const title = document.createElement("h3");
        title.textContent = `${food.name} - ₹${food.price}`;

        const button = document.createElement("button");
        button.textContent = "Add to Cart";
        button.onclick = () => {
            cart[food.id] = (cart[food.id] || 0) + 1;
            showCart();
        };

        div.append(title, button);
        menu.appendChild(div);
    });
}

function showCart() {
    const list = document.getElementById("cart");
    list.innerHTML = "";
    let total = 0;

    foods.forEach(food => {
        const quantity = cart[food.id] || 0;
        if (quantity === 0) return;

        total += food.price * quantity;

        const li = document.createElement("li");
        li.textContent =
            `${food.name} x ${quantity} = ₹${food.price * quantity}`;

        const remove = document.createElement("button");
        remove.textContent = "Remove one";
        remove.onclick = () => {
            cart[food.id]--;
            if (cart[food.id] === 0) delete cart[food.id];
            showCart();
        };

        li.appendChild(remove);
        list.appendChild(li);
    });

    document.getElementById("total").textContent =
        `Total: ₹${total}`;
}

async function placeOrder() {
    const items = Object.entries(cart).map(([foodId, quantity]) => ({
        foodId: Number(foodId),
        quantity
    }));

    if (items.length === 0) {
        alert("Please add food to your cart.");
        return;
    }

    try {
        const response = await fetch("/api/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ items })
        });

        if (!response.ok) {
            throw new Error("Unable to place order");
        }

        const order = await response.json();

        alert(
            `Order #${order.orderId} confirmed!\n` +
            `Total: ₹${order.total}`
        );

        cart = {};
        showCart();
    } catch (error) {
        alert(error.message);
    }
}

loadFoods();
