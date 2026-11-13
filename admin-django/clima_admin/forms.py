from django import forms
from django.contrib.auth import get_user_model
from django.contrib.auth.forms import AuthenticationForm
from django.contrib.auth.forms import UserCreationForm


class PanelAuthenticationForm(AuthenticationForm):
    """Authenticate a Django account and verify the selected panel role."""

    account_type = forms.ChoiceField(
        label="Tipo de acceso",
        choices=(("usuario", "Usuario"), ("admin", "Administrador")),
        initial="usuario",
        widget=forms.RadioSelect,
    )

    def clean(self):
        cleaned_data = super().clean()
        user = self.get_user()
        account_type = cleaned_data.get("account_type")

        if user is None or account_type is None:
            return cleaned_data

        if account_type == "admin" and not user.is_staff:
            self.add_error("account_type", "Esta cuenta no tiene permisos de administrador.")
        elif account_type == "usuario" and user.is_staff:
            self.add_error("account_type", "Selecciona Administrador para esta cuenta.")

        return cleaned_data


class UserRegistrationForm(UserCreationForm):
    email = forms.EmailField(label="Correo electrónico", required=True)

    class Meta(UserCreationForm.Meta):
        model = get_user_model()
        fields = ("username", "email")

    def save(self, commit=True):
        user = super().save(commit=False)
        user.email = self.cleaned_data["email"]
        user.is_staff = False
        user.is_superuser = False
        if commit:
            user.save()
        return user
